// UserController.java (текущий пользователь)
package com.nester.controllers;

import com.nester.dto.ResponseResult;
import com.nester.security.jwt.JwtUser;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public void getCurrentUser(Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        if (auth == null || !auth.isAuthenticated()) {
            response.setStatus(401);
            response.getWriter().write("{\"message\":\"Не авторизован\",\"data\":null}");
            return;
        }

        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        var user = userService.findById(jwtUser.getId());
        response.getWriter().write("{\"message\":null,\"data\":{\"id\":\"" + user.getId() +
                "\",\"login\":\"" + user.getLogin() + "\",\"fullName\":\"" + user.getFullName() +
                "\",\"role\":\"" + user.getRole() + "\",\"active\":" + user.isActive() + "}}");
    }
}