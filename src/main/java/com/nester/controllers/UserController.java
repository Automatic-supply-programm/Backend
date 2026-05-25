// UserController.java (текущий пользователь)
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.User;
import com.nester.security.jwt.JwtUser;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    public void getCurrentUser(Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        if (auth == null || !auth.isAuthenticated()) {
            response.setStatus(401);
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Не авторизован", null));
            return;
        }

        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        User user = userService.findById(jwtUser.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("login", user.getLogin());
        data.put("fullName", user.getFullName());
        data.put("role", user.getRole());
        data.put("active", user.isActive());
        data.put("warehouseId", user.getWarehouseId());
        data.put("productionLineIds", user.getProductionLineIds());
        data.put("managedWarehouseIds", user.getManagedWarehouseIds());

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, data));
    }

    // Справочник пользователей для заполнения dropdown-полей в формах
    // Доступен всем авторизованным пользователям; возвращает только нечувствительные поля
    @GetMapping("/list")
    public void getUsersList(@RequestParam(required = false) String role,
                             HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<User> users = userService.findAll().stream()
                .filter(User::isActive)
                .collect(Collectors.toList());
        if (role != null && !role.isEmpty()) {
            users = users.stream().filter(u -> role.equals(u.getRole())).collect(Collectors.toList());
        }
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("fullName", u.getFullName());
            item.put("login", u.getLogin());
            item.put("role", u.getRole());
            item.put("warehouseId", u.getWarehouseId());
            item.put("productionLineIds", u.getProductionLineIds());
            item.put("managedWarehouseIds", u.getManagedWarehouseIds());
            return item;
        }).collect(Collectors.toList());
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, result));
    }

    // Справочник складов: уникальные warehouseId из профилей активных работников
    @GetMapping("/warehouses")
    public void getWarehousesList(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<Map<String, Object>> result = userService.findAll().stream()
                .filter(u -> u.isActive() && "WORKER".equals(u.getRole()) && u.getWarehouseId() != null)
                .map(u -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("warehouseId", u.getWarehouseId());
                    item.put("workerName", u.getFullName());
                    item.put("workerId", u.getId());
                    return item;
                })
                // Deduplicate by warehouseId, keeping first encountered
                .collect(java.util.stream.Collectors.toMap(
                        m -> (String) m.get("warehouseId"),
                        m -> m,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .collect(Collectors.toList());
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, result));
    }
}
