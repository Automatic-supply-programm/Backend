package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.model.User;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
import java.util.Set;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final EventLogService eventLogService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping("/event-logs")
    public void getEventLogsForManager(Authentication auth,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate,
                                       @RequestParam(required = false) String eventType,
                                       @RequestParam(required = false) String userId,
                                       HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        User manager = userService.findById(jwtUser.getId());
        List<String> managedWarehouseIds = manager.getManagedWarehouseIds();
        if (managedWarehouseIds == null || managedWarehouseIds.isEmpty()) {
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, List.of()));
            return;
        }

        List<EventLog> allEvents = eventLogService.findAllWithFilters(userId, eventType, startDate, endDate);
        List<EventLog> filtered = allEvents.stream()
                .filter(e -> !EventLogService.SYSTEM_EVENT_TYPES.contains(e.getEventType()))
                .filter(e -> e.getWarehouseId() != null && managedWarehouseIds.contains(e.getWarehouseId()))
                .collect(Collectors.toList());

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, filtered));
    }

    // Метод для страницы "Пользователи" (требование ТЗ)
    @GetMapping("/users")
    public void getAllUsers(Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        // Менеджер может видеть всех пользователей (или только подчинённых – упрощённо всех)
        List<User> users = userService.findAll();
        // Скрываем пароли и другую чувствительную информацию
        users.forEach(u -> u.setPassword(null));
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, users));
    }
}