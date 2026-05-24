// RequestServiceImpl.java
package com.nester.service;

import com.nester.model.Material;
import com.nester.model.MaterialBatch;
import com.nester.model.Request;
import com.nester.model.RequestItem;
import com.nester.repository.MaterialRepository;
import com.nester.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final MaterialRepository materialRepository;

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
        request.setNumber("REQ-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4));
        request.setStatus("UNDER_CONSIDERATION");
        request.setCreatedDate(LocalDateTime.now());
        request.setArchived(false);
        return requestRepository.save(request);
    }

    @Override
    public Request update(String id, Request request, String userId) {
        Request existing = findById(id);
        if (!existing.getRequesterId().equals(userId)) {
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
    public Request changeStatus(String id, String status, String comment, String userId, String userName) {
        Request request = findById(id);
        validateStatusTransition(request, status, userId);

        if ((status.equals("APPROVED") || status.equals("REJECTED") || status.equals("SENT_FOR_REVISION"))
                && !request.getDestinationId().equals(userId)) {
            throw new SecurityException("Только адресат может менять статус заявки");
        }

        request.setStatus(status);
        if (comment != null && !comment.isEmpty()) {
            request.setComment(comment);
        }

        // Для ISSUE: при APPROVED не списываем, а переводим в WAITING_CONFIRMATION
        if ("ISSUE".equals(request.getType()) && status.equals("APPROVED")) {
            request.setStatus("WAITING_CONFIRMATION");
        }

        // Для REPLENISHMENT/RECEIPT/RETURN: при APPROVED увеличиваем остатки
        if (status.equals("APPROVED") &&
                (request.getType().equals("REPLENISHMENT") || request.getType().equals("RECEIPT") || request.getType().equals("RETURN"))) {
            for (RequestItem item : request.getItems()) {
                Material material = materialRepository.findById(item.getMaterialId()).orElse(null);
                if (material != null) {
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
            } else if ("APPROVED".equals(currentStatus)) {
                if (!"WAITING_CONFIRMATION".equals(newStatus)) {
                    throw new IllegalArgumentException("Одобренную заявку можно только подтвердить");
                }
            } else if ("WAITING_CONFIRMATION".equals(currentStatus)) {
                if (!"CONFIRMED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Заявку можно подтвердить или отменить");
                }
            }
        }

        if ("REPLENISHMENT".equals(type) || "RECEIPT".equals(type) || "RETURN".equals(type)) {
            if ("UNDER_CONSIDERATION".equals(currentStatus)) {
                if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus) &&
                        !"SENT_FOR_REVISION".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new IllegalArgumentException("Недопустимый переход статуса");
                }
            }
        }
    }

    @Override
    @Transactional
    public Request confirmReceipt(String id, String userId, String userName) {
        Request request = findById(id);
        if (!"ISSUE".equals(request.getType())) {
            throw new IllegalArgumentException("Подтверждение получения доступно только для заявок на выдачу");
        }
        if (!"WAITING_CONFIRMATION".equals(request.getStatus())) {
            throw new IllegalArgumentException("Подтвердить можно только заявку со статусом 'Одобрена'");
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
        if ("CONFIRMED".equals(request.getStatus())) {
            throw new IllegalArgumentException("Нельзя архивировать подтвержденную заявку");
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
}