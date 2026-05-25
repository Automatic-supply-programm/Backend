// RequestController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.model.Request;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
import com.nester.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService requestService;
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Получение заявок, созданных текущим пользователем (EMPLOYEE, WORKER)
    @GetMapping
    public void getAll(@RequestParam(defaultValue = "false") boolean archived,
                       Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        List<Request> requests = requestService.findByRequester(jwtUser.getId());
        if (!archived) {
            requests = requests.stream().filter(r -> !r.isArchived()).toList();
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, requests));
    }

    // Входящие заявки (для WORKER и MANAGER) с фильтрацией
    @GetMapping("/incoming")
    public void getIncoming(@RequestParam(defaultValue = "false") boolean archived,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            @RequestParam(required = false) String sourceId,
                            Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        List<Request> requests = requestService.findByDestination(jwtUser.getId());
        if (!archived) {
            requests = requests.stream().filter(r -> !r.isArchived()).collect(Collectors.toList());
        }
        if (sourceId != null && !sourceId.isEmpty()) {
            requests = requests.stream().filter(r -> sourceId.equals(r.getSourceId())).collect(Collectors.toList());
        }
        requests = filterRequests(requests, search, type, status, startDate, endDate);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, requests));
    }

    // Все заявки (только для ADMIN) с фильтрацией
    @GetMapping("/all")
    public void getAllRequests(@RequestParam(defaultValue = "false") boolean archived,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) String sourceId,
                               Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        if (!jwtUser.getAuthorities().iterator().next().getAuthority().equals("ROLE_ADMIN")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }
        List<Request> requests = requestService.findAll(archived);
        if (sourceId != null && !sourceId.isEmpty()) {
            requests = requests.stream().filter(r -> sourceId.equals(r.getSourceId())).collect(Collectors.toList());
        }
        requests = filterRequests(requests, search, type, status, startDate, endDate);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, requests));
    }

    // Общий метод фильтрации
    private List<Request> filterRequests(List<Request> requests, String search, String type, String status,
                                         String startDate, String endDate) {
        if (search != null && !search.isEmpty()) {
            requests = requests.stream()
                    .filter(r -> (r.getNumber() != null && r.getNumber().contains(search)) ||
                            (r.getRequesterName() != null && r.getRequesterName().contains(search)) ||
                            r.getItems().stream().anyMatch(i -> i.getMaterialName() != null && i.getMaterialName().contains(search)))
                    .collect(Collectors.toList());
        }
        if (type != null && !type.isEmpty()) {
            requests = requests.stream().filter(r -> r.getType().equals(type)).collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            requests = requests.stream().filter(r -> r.getStatus().equals(status)).collect(Collectors.toList());
        }
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
            requests = requests.stream().filter(r -> !r.getCreatedDate().isBefore(start)).collect(Collectors.toList());
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59);
            requests = requests.stream().filter(r -> !r.getCreatedDate().isAfter(end)).collect(Collectors.toList());
        }
        return requests;
    }

    // Получение заявки по ID
    @GetMapping("/{id}")
    public void getById(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            Request request = requestService.findById(id);
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, request));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    // Создание заявки
    @PostMapping
    public void create(@RequestBody Request request, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            request.setRequesterId(jwtUser.getId());
            request.setRequesterName(jwtUser.getFullName());
            request.setRequesterRole(jwtUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));

            Request created = requestService.create(request);

            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("REQUEST_CREATED");
            eventLog.setObjectType("REQUEST");
            eventLog.setObjectId(created.getId());
            eventLog.setObjectNumber(created.getNumber());
            eventLog.setWarehouseId(created.getSourceId());
            eventLog.setDescription("Создана заявка типа " + created.getType() + " от " + created.getRequesterName());
            eventLog.setResult("CREATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, created));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    // Редактирование заявки
    @PutMapping("/{id}")
    public void update(@PathVariable String id, @RequestBody Request request, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            Request updated = requestService.update(id, request, jwtUser.getId());

            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("REQUEST_UPDATED");
            eventLog.setObjectType("REQUEST");
            eventLog.setObjectId(updated.getId());
            eventLog.setObjectNumber(updated.getNumber());
            eventLog.setDescription("Изменена заявка " + updated.getNumber());
            eventLog.setResult("UPDATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException | SecurityException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    // Изменение статуса заявки
    @PostMapping("/{id}/status")
    public void changeStatus(@PathVariable String id,
                             @RequestParam String status,
                             @RequestParam(required = false) String comment,
                             Authentication auth,
                             HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            Request updated = requestService.changeStatus(id, status, comment, jwtUser.getId(), jwtUser.getUsername());

            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("REQUEST_STATUS_CHANGED");
            eventLog.setObjectType("REQUEST");
            eventLog.setObjectId(updated.getId());
            eventLog.setObjectNumber(updated.getNumber());
            eventLog.setWarehouseId(updated.getSourceId());
            eventLog.setDescription("Изменен статус заявки " + updated.getNumber() + " на " + status);
            if (comment != null) {
                eventLog.setDescription(eventLog.getDescription() + ". Комментарий: " + comment);
            }
            eventLog.setResult(status);
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException | SecurityException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    // Подтверждение получения материалов (для EMPLOYEE)
    @PostMapping("/{id}/confirm")
    public void confirmReceipt(@PathVariable String id, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            Request updated = requestService.confirmReceipt(id, jwtUser.getId(), jwtUser.getUsername());

            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("RECEIPT_CONFIRMED");
            eventLog.setObjectType("REQUEST");
            eventLog.setObjectId(updated.getId());
            eventLog.setObjectNumber(updated.getNumber());
            eventLog.setWarehouseId(updated.getSourceId());
            eventLog.setDescription("Подтверждено получение материалов по заявке " + updated.getNumber());
            eventLog.setResult("CONFIRMED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException | SecurityException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    // Архивирование заявки
    @PostMapping("/{id}/archive")
    public void archive(@PathVariable String id, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            Request archived = requestService.archive(id, jwtUser.getId());

            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("REQUEST_ARCHIVED");
            eventLog.setObjectType("REQUEST");
            eventLog.setObjectId(archived.getId());
            eventLog.setObjectNumber(archived.getNumber());
            eventLog.setDescription("Архивирована заявка " + archived.getNumber());
            eventLog.setResult("ARCHIVED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, archived));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }
}