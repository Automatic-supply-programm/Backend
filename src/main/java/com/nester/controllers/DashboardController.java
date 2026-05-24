// DashboardController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
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
                stats.put("pendingRequests", requestService.findByDestination(jwtUser.getId()).size());
                stats.put("totalMaterials", materialService.count());
                stats.put("deficitMaterials", materialService.getDeficitMaterials().size());
            }
            case "EMPLOYEE" -> {
                stats.put("myRequests", requestService.findByRequester(jwtUser.getId()).size());
                stats.put("underConsideration", requestService.findByRequester(jwtUser.getId()).stream()
                        .filter(r -> "UNDER_CONSIDERATION".equals(r.getStatus())).count());
            }
            case "MANAGER" -> {
                stats.put("pendingApproval", requestService.findByDestination(jwtUser.getId()).size());
                stats.put("deficitMaterials", materialService.getDeficitMaterials().size());
            }
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, stats));
    }

    // Новый эндпоинт для таблицы "Последние действия пользователей" (требование ТЗ для админа)
    @GetMapping("/recent")
    public void getRecentActions(@RequestParam(defaultValue = "10") int limit,
                                 Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        String role = jwtUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        if (!"ADMIN".equals(role)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }
        List<EventLog> recentEvents = eventLogService.findAllWithFilters(null, null, null, null);
        if (recentEvents.size() > limit) {
            recentEvents = recentEvents.subList(0, limit);
        }
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, recentEvents));
    }
}