// AdminController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.dto.UserCreateRequest;
import com.nester.model.EventLog;
import com.nester.model.User;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final EventLogService eventLogService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @GetMapping("/users")
    public void getAllUsers(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<User> users = userService.findAll();
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, users));
    }

    @GetMapping("/users/{id}")
    public void getUser(@PathVariable String id, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            User user = userService.findById(id);
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, user));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PostMapping("/users")
    public void createUser(@RequestBody UserCreateRequest userRequest,
                           Authentication auth,
                           HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            User user = new User();
            user.setLogin(userRequest.getLogin());
            user.setFullName(userRequest.getFullName());
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            user.setRole(userRequest.getRole());
            user.setActive(true);

            if (userRequest.getWarehouseId() != null) {
                user.setWarehouseId(userRequest.getWarehouseId());
            }
            if (userRequest.getProductionLineIds() != null) {
                user.setProductionLineIds(userRequest.getProductionLineIds());
            }
            if (userRequest.getManagedWarehouseIds() != null) {
                user.setManagedWarehouseIds(userRequest.getManagedWarehouseIds());
            }

            User created = userService.create(user);

            // Логируем создание пользователя
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole("ADMIN");
            eventLog.setEventType("USER_CREATED");
            eventLog.setObjectType("USER");
            eventLog.setObjectId(created.getId());
            eventLog.setDescription("Создан пользователь " + created.getFullName() + " с ролью " + created.getRole());
            eventLog.setResult("CREATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, created));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PutMapping("/users/{id}")
    public void updateUser(@PathVariable String id, @RequestBody User user,
                           Authentication auth,
                           HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            User updated = userService.update(id, user);

            // Логируем изменение
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole("ADMIN");
            eventLog.setEventType("USER_UPDATED");
            eventLog.setObjectType("USER");
            eventLog.setObjectId(updated.getId());
            eventLog.setDescription("Изменены данные пользователя " + updated.getFullName());
            eventLog.setResult("UPDATED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, updated));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PostMapping("/users/{id}/change-password")
    public void changePassword(@PathVariable String id,
                               @RequestParam String newPassword,
                               Authentication auth,
                               HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            User user = userService.findById(id);
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.update(id, user);

            // Логируем смену пароля
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole("ADMIN");
            eventLog.setEventType("PASSWORD_CHANGED");
            eventLog.setObjectType("USER");
            eventLog.setObjectId(user.getId());
            eventLog.setDescription("Смена пароля для пользователя " + user.getFullName());
            eventLog.setResult("CHANGED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, "Пароль успешно изменен"));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }

    @PostMapping("/users/{id}/toggle")
    public void toggleUserActive(@PathVariable String id,
                                 @RequestParam boolean active,
                                 Authentication auth,
                                 HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        User user = userService.changeActive(id, active);

        // Логируем изменение статуса доступа
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        EventLog eventLog = new EventLog();
        eventLog.setTimestamp(LocalDateTime.now());
        eventLog.setUserId(jwtUser.getId());
        eventLog.setUserFullName(jwtUser.getUsername());
        eventLog.setUserRole("ADMIN");
        eventLog.setEventType(active ? "ACCESS_RESTORED" : "ACCESS_SUSPENDED");
        eventLog.setObjectType("USER");
        eventLog.setObjectId(user.getId());
        eventLog.setDescription((active ? "Восстановлен" : "Приостановлен") + " доступ пользователя " + user.getFullName());
        eventLog.setResult(active ? "RESTORED" : "SUSPENDED");
        eventLogService.save(eventLog);

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, user));
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable String id, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            User deleted = userService.delete(id);

            // Логируем удаление
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getUsername());
            eventLog.setUserRole("ADMIN");
            eventLog.setEventType("USER_DELETED");
            eventLog.setObjectType("USER");
            eventLog.setObjectId(deleted.getId());
            eventLog.setDescription("Удален пользователь " + deleted.getFullName());
            eventLog.setResult("DELETED");
            eventLogService.save(eventLog);

            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, deleted));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>(e.getMessage(), null));
        }
    }
}