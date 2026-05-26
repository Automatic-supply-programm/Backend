// RequestServiceImpl.java
package com.nester.service;

import com.nester.model.Material;
import com.nester.model.MaterialBatch;
import com.nester.model.Request;
import com.nester.model.RequestItem;
import com.nester.model.User;
import com.nester.repository.MaterialRepository;
import com.nester.repository.RequestRepository;
import com.nester.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    @Override
    public List<Request> findAll(boolean includeArchived) {
        if (includeArchived) {
            return requestRepository.findAll();
        }
        return requestRepository.findByArchived(false);
    }

    @Override
    public Request findById(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
    }

    @Override
    public Request create(Request request) {
        // Проверяем, что destinationId ссылается на реального активного пользователя
        if (request.getDestinationId() != null && !request.getDestinationId().isEmpty()) {
            User destination = userRepository.findById(request.getDestinationId())
                    .filter(u -> !u.isDeleted() && u.isActive())
                    .orElseThrow(() -> new IllegalArgumentException("Получатель с указанным ID не найден или неактивен"));
            if ("RECEIPT".equals(request.getType()) && !"MANAGER".equals(destination.getRole())) {
                throw new IllegalArgumentException("Заявку на поступление можно направить только менеджеру");
            }
            request.setDestinationName(destination.getFullName());
        }
        if ("RETURN".equals(request.getType()) && request.getRequesterId() != null && request.getItems() != null) {
            validateReturnQuantities(request.getRequesterId(), request.getItems());
        }
        request.setNumber("REQ-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4));
        request.setStatus("UNDER_CONSIDERATION");
        request.setCreatedDate(LocalDateTime.now());
        request.setArchived(false);
        return requestRepository.save(request);
    }

    @Override
    public Request update(String id, Request request, String userId, String userRole) {
        Request existing = findById(id);
        if (!"ROLE_ADMIN".equals(userRole) && !existing.getRequesterId().equals(userId)) {
            throw new SecurityException("Нельзя редактировать чужую заявку");
        }
        if (!"UNDER_CONSIDERATION".equals(existing.getStatus()) &&
                !"SENT_FOR_REVISION".equals(existing.getStatus())) {
            throw new IllegalArgumentException("Заявку можно редактировать только в статусе 'На рассмотрении' или 'Отправлена для доработки'");
        }
        existing.setItems(request.getItems());
        existing.setComment(request.getComment());
        existing.setSourceId(request.getSourceId());
        existing.setSourceName(request.getSourceName());
        existing.setDestinationId(request.getDestinationId());
        existing.setDestinationName(request.getDestinationName());
        return requestRepository.save(existing);
    }

    @Override
    @Transactional
    public Request changeStatus(String id, String status, String comment, String userId, String userName, String userRole) {
        Request request = findById(id);
        validateStatusTransition(request, status, userId);

        boolean isAdmin = "ROLE_ADMIN".equals(userRole);

        if (!isAdmin && (status.equals("APPROVED") || status.equals("REJECTED") || status.equals("SENT_FOR_REVISION") || status.equals("ACCEPTED"))
                && (request.getDestinationId() == null || !request.getDestinationId().equals(userId))) {
            throw new SecurityException("Только адресат может менять статус заявки");
        }

        if (!isAdmin && status.equals("CANCELLED") && !request.getRequesterId().equals(userId)) {
            throw new SecurityException("Отменить заявку может только её автор");
        }

        request.setStatus(status);
        if (comment != null && !comment.isEmpty()) {
            request.setComment(comment);
        }

        // Отменённая заявка автоматически архивируется (требование ТЗ)
        if ("CANCELLED".equals(status)) {
            request.setArchived(true);
        }

        // Для ISSUE: при APPROVED не списываем, а переводим в WAITING_CONFIRMATION
        if ("ISSUE".equals(request.getType()) && status.equals("APPROVED")) {
            request.setStatus("WAITING_CONFIRMATION");
        }

        // REPLENISHMENT — заявка на закупку: остатки НЕ меняются при одобрении.
        // RECEIPT (поступление от поставщика) — при APPROVED создаём партию и увеличиваем остаток.
        // RETURN (возврат с участка) — при ACCEPTED создаём партию и увеличиваем остаток.
        boolean isReceiptApproved = "APPROVED".equals(status) && "RECEIPT".equals(request.getType());
        boolean isReturnAccepted = "ACCEPTED".equals(status) && "RETURN".equals(request.getType());
        if (isReceiptApproved || isReturnAccepted) {
            for (RequestItem item : request.getItems()) {
                Material material = materialRepository.findById(item.getMaterialId()).orElse(null);
                if (material != null) {
                    MaterialBatch batch = new MaterialBatch();
                    batch.setBatchNumber("BATCH-" + request.getNumber() + "-" + item.getMaterialId().substring(0, Math.min(4, item.getMaterialId().length())));
                    batch.setReceiptDate(LocalDateTime.now());
                    batch.setInitialQuantity(item.getQuantity());
                    batch.setCurrentQuantity(item.getQuantity());
                    batch.setStorageLocation(item.getExactLocation());
                    batch.setReceiptActNumber(item.getReceiptActNumber());
                    batch.setExpiryDate(item.getExpiryDate());
                    batch.setAcceptedByUserId(userId);
                    batch.setAcceptedByName(userName);
                    material.getBatches().add(batch);
                    material.setCurrentStock(material.getCurrentStock() + item.getQuantity());
                    material.setLastReceiptDate(LocalDateTime.now());
                    materialRepository.save(material);
                }
            }
        }

        return requestRepository.save(request);
    }

    private void validateStatusTransition(Request request, String newStatus, String userId) {
        String currentStatus = request.getStatus();
        String type = request.getType();

        if (currentStatus.equals(newStatus)) {
            throw new IllegalArgumentException("Заявка уже в статусе " + newStatus);
        }

        if ("ISSUE".equals(type)) {
            if ("UNDER_CONSIDERATION".equals(currentStatus)) {
                if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Недопустимый переход статуса для заявки на выдачу");
                }
            } else if ("WAITING_CONFIRMATION".equals(currentStatus)) {
                // WAITING_CONFIRMATION — заявка одобрена работником, ожидает подтверждения участком
                // Подтверждение идёт через /confirm, отмена — через /status?status=CANCELLED
                if (!"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Одобренную заявку можно только подтвердить через /confirm или отменить");
                }
            }
        }

        if ("REPLENISHMENT".equals(type) || "RECEIPT".equals(type)) {
            if ("UNDER_CONSIDERATION".equals(currentStatus)) {
                if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus) &&
                        !"SENT_FOR_REVISION".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Недопустимый переход статуса");
                }
            } else if ("SENT_FOR_REVISION".equals(currentStatus)) {
                if (!"UNDER_CONSIDERATION".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("После доработки заявку можно только отправить на рассмотрение или отменить");
                }
            }
        }

        // RETURN (возврат с участка → работник склада): только «Принята» (ACCEPTED) или «Отменена»
        if ("RETURN".equals(type)) {
            if ("UNDER_CONSIDERATION".equals(currentStatus)) {
                if (!"ACCEPTED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Заявку на возврат можно только принять или отменить");
                }
            }
        }
    }

    @Override
    @Transactional
    public Request confirmReceipt(String id, String userId, String userName, String userRole) {
        Request request = findById(id);
        if (!"ISSUE".equals(request.getType())) {
            throw new IllegalArgumentException("Подтверждение получения доступно только для заявок на выдачу");
        }
        if (!"WAITING_CONFIRMATION".equals(request.getStatus())) {
            throw new IllegalArgumentException("Подтвердить можно только заявку со статусом 'Ожидает подтверждения'");
        }
        if (!"ROLE_ADMIN".equals(userRole) && !request.getRequesterId().equals(userId)) {
            throw new SecurityException("Подтвердить получение может только автор заявки");
        }

        // Списание материалов по FIFO из партий с заполнением warehouseId для логирования
        for (RequestItem item : request.getItems()) {
            Material material = materialRepository.findById(item.getMaterialId())
                    .orElseThrow(() -> new IllegalArgumentException("Материал не найден: " + item.getMaterialName()));

            double remaining = item.getQuantity();
            List<MaterialBatch> sortedBatches = material.getBatches().stream()
                    .filter(b -> b.getCurrentQuantity() > 0)
                    .sorted(Comparator.comparing(MaterialBatch::getReceiptDate))
                    .collect(Collectors.toList());

            for (MaterialBatch batch : sortedBatches) {
                if (remaining <= 0) break;
                double available = batch.getCurrentQuantity();
                double toDeduct = Math.min(available, remaining);
                batch.setCurrentQuantity(available - toDeduct);
                remaining -= toDeduct;
            }

            if (remaining > 0) {
                throw new IllegalArgumentException("Недостаточно материала " + material.getName() + " на складе");
            }

            material.setCurrentStock(material.getCurrentStock() - item.getQuantity());
            material.setLastIssueDate(LocalDateTime.now());
            materialRepository.save(material);
        }

        request.setStatus("CONFIRMED");
        return requestRepository.save(request);
    }

    @Override
    public Request archive(String id, String userId) {
        Request request = findById(id);
        if ("UNDER_CONSIDERATION".equals(request.getStatus()) || "WAITING_CONFIRMATION".equals(request.getStatus())) {
            throw new IllegalArgumentException("Нельзя архивировать заявку в статусе 'На рассмотрении' или 'Ожидает подтверждения'");
        }
        request.setArchived(true);
        return requestRepository.save(request);
    }

    @Override
    public long countActive() {
        return requestRepository.countByArchived(false);
    }

    @Override
    public List<Request> findByRequester(String requesterId) {
        return requestRepository.findByRequesterId(requesterId);
    }

    @Override
    public List<Request> findByDestination(String destinationId) {
        return requestRepository.findByDestinationId(destinationId);
    }

    private void validateReturnQuantities(String requesterId, List<RequestItem> returnItems) {
        List<Request> issued = requestRepository.findByRequesterIdAndType(requesterId, "ISSUE");
        List<Request> returned = requestRepository.findByRequesterIdAndType(requesterId, "RETURN");

        Map<String, Double> available = new HashMap<>();
        for (Request r : issued) {
            if (!"CONFIRMED".equals(r.getStatus()) || r.getItems() == null) continue;
            for (RequestItem item : r.getItems()) {
                available.merge(item.getMaterialId(), item.getQuantity(), Double::sum);
            }
        }
        for (Request r : returned) {
            if (!"ACCEPTED".equals(r.getStatus()) || r.getItems() == null) continue;
            for (RequestItem item : r.getItems()) {
                available.merge(item.getMaterialId(), -item.getQuantity(), Double::sum);
            }
        }
        for (RequestItem item : returnItems) {
            double qty = available.getOrDefault(item.getMaterialId(), 0.0);
            if (item.getQuantity() > qty) {
                throw new IllegalArgumentException(
                    "Недостаточно материала «" + item.getMaterialName() + "» на участке. " +
                    "Доступно: " + qty + " " + item.getUnit());
            }
        }
    }
}