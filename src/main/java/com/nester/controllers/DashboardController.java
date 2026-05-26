// DashboardController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
import java.util.Set;
import com.nester.service.MaterialService;
import com.nester.service.RequestService;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final UserService userService;
    private final MaterialService materialService;
    private final RequestService requestService;
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public void getStats(Authentication auth, HttpServletResponse response) throws IOException {
        // ... существующий код без изменений
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        String role = jwtUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        Map<String, Object> stats = new HashMap<>();
        stats.put("role", role);
        switch (role) {
            case "ADMIN" -> {
                stats.put("totalUsers", userService.findAll().size());
                stats.put("activeUsers", userService.getActiveUsersCount());
                stats.put("totalMaterials", materialService.count());
                stats.put("activeRequests", requestService.countActive());
                stats.put("deficitMaterials", materialService.getDeficitMaterials().size());
            }
            case "WORKER" -> {
                com.nester.model.User worker = userService.findById(jwtUser.getId());
                stats.put("warehouseId", worker.getWarehouseId());
                List<com.nester.model.Request> workerIncoming = requestService.findByDestination(jwtUser.getId());
                // Заявки на выдачу (ISSUE) от участков, ожидающие обработки
                stats.put("pendingRequests", workerIncoming.stream()
                        .filter(r -> "UNDER_CONSIDERATION".equals(r.getStatus()) && !r.isArchived()
                                  && "ISSUE".equals(r.getType())).count());
                // Поступления и возвраты, ожидающие оформления (RECEIPT/RETURN в UNDER_CONSIDERATION + созданные самим WORKER)
                List<com.nester.model.Request> workerCreated = requestService.findByRequester(jwtUser.getId());
                stats.put("pendingReceipts", workerCreated.stream()
                        .filter(r -> !r.isArchived() && "RECEIPT".equals(r.getType())
                                  && "UNDER_CONSIDERATION".equals(r.getStatus())).count());
                stats.put("totalMaterials", materialService.count());
                stats.put("deficitMaterials", materialService.getDeficitMaterials().size());
            }
            case "EMPLOYEE" -> {
                com.nester.model.User employee = userService.findById(jwtUser.getId());
                stats.put("warehouseId", employee.getWarehouseId());
                stats.put("productionLineIds", employee.getProductionLineIds());
                List<com.nester.model.Request> myRequests = requestService.findByRequester(jwtUser.getId());
                stats.put("myRequests", myRequests.stream().filter(r -> !r.isArchived()).count());
                stats.put("underConsideration", myRequests.stream()
                        .filter(r -> "UNDER_CONSIDERATION".equals(r.getStatus())).count());
                stats.put("waitingConfirmation", myRequests.stream()
                        .filter(r -> "WAITING_CONFIRMATION".equals(r.getStatus())).count());
                stats.put("rejected", myRequests.stream()
                        .filter(r -> "REJECTED".equals(r.getStatus())).count());
            }
            case "MANAGER" -> {
                List<com.nester.model.Request> managerIncoming = requestService.findByDestination(jwtUser.getId());
                stats.put("totalReplenishment", managerIncoming.stream()
                        .filter(r -> "REPLENISHMENT".equals(r.getType()) && !r.isArchived()).count());
                stats.put("totalReceipts", managerIncoming.stream()
                        .filter(r -> "RECEIPT".equals(r.getType()) && !r.isArchived()).count());
                stats.put("pendingApproval", managerIncoming.stream()
                        .filter(r -> "UNDER_CONSIDERATION".equals(r.getStatus()) && !r.isArchived()).count());
                stats.put("approved", managerIncoming.stream()
                        .filter(r -> "APPROVED".equals(r.getStatus()) && !r.isArchived()).count());
                stats.put("rejected", managerIncoming.stream()
                        .filter(r -> "REJECTED".equals(r.getStatus()) && !r.isArchived()).count());
                stats.put("deficitMaterials", materialService.getDeficitMaterials().size());
                com.nester.model.User manager = userService.findById(jwtUser.getId());
                stats.put("managedWarehouseIds", manager.getManagedWarehouseIds());
            }
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, stats));
    }

    // Таблица "Последние действия пользователей" (ADMIN) / "Последние складские операции" (WORKER)
    @GetMapping("/recent")
    public void getRecentActions(@RequestParam(defaultValue = "10") int limit,
                                 Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        String role = jwtUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        List<EventLog> recentEvents;
        if ("ADMIN".equals(role)) {
            // Администратор видит все события
            recentEvents = eventLogService.findAllWithFilters(null, null, null, null);
        } else if ("WORKER".equals(role)) {
            // Работник видит только бизнес-события по своему складу (без системных)
            com.nester.model.User worker = userService.findById(jwtUser.getId());
            String workerWarehouseId = worker.getWarehouseId();
            List<EventLog> allEvents = eventLogService.findAllWithFilters(null, null, null, null);
            final String wId = jwtUser.getId();
            recentEvents = allEvents.stream()
                    .filter(e -> !EventLogService.SYSTEM_EVENT_TYPES.contains(e.getEventType()))
                    .filter(e -> wId.equals(e.getUserId()) ||
                                 (workerWarehouseId != null && workerWarehouseId.equals(e.getWarehouseId())))
                    .collect(Collectors.toList());
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }

        if (recentEvents.size() > limit) {
            recentEvents = recentEvents.subList(0, limit);
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, recentEvents));
    }
}