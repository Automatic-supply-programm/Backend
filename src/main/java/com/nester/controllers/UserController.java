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
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
}
