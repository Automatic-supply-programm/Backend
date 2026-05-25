// AuthController.java
package com.nester.controllers;

import com.nester.dto.LoginRequest;
import org.springframework.security.core.Authentication;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.model.User;
import com.nester.security.jwt.JwtTokenProvider;
import com.nester.security.jwt.JwtUser;
import com.nester.service.EventLogService;
import com.nester.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final EventLogService eventLogService;

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getLogin(), loginRequest.getPassword())
            );

            User user = userService.findByLogin(loginRequest.getLogin());

            if (!user.isActive()) {
                response.setStatus(403);
                response.getWriter().write("{\"message\":\"Доступ приостановлен\",\"data\":null}");
                return;
            }

            // Обновляем время последнего входа
            user.setLastLogin(LocalDateTime.now());
            userService.updateLastLogin(user.getId(), user.getLastLogin());  // вместо userService.update(...)

            // Логируем вход
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(user.getId());
            eventLog.setUserFullName(user.getFullName());
            eventLog.setUserRole(user.getRole());
            eventLog.setEventType("LOGIN");
            eventLog.setDescription("Пользователь вошел в систему");
            eventLog.setResult("SUCCESS");
            eventLogService.save(eventLog);

            String token = jwtTokenProvider.createToken(user.getId(), loginRequest.getLogin(), user.getRole());
            response.getWriter().write("{\"message\":null,\"data\":\"" + token + "\"}");
        } catch (Exception e) {
            // Логируем неудачную попытку
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setEventType("LOGIN_FAILED");
            eventLog.setDescription("Неудачная попытка входа: " + loginRequest.getLogin());
            eventLog.setResult("FAILED");
            eventLogService.save(eventLog);

            response.setStatus(401);
            response.getWriter().write("{\"message\":\"Неверный логин или пароль\",\"data\":null}");
        }
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String token,
                       Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        if (auth != null && auth.isAuthenticated()) {
            JwtUser jwtUser = (JwtUser) auth.getPrincipal();
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(LocalDateTime.now());
            eventLog.setUserId(jwtUser.getId());
            eventLog.setUserFullName(jwtUser.getFullName());
            eventLog.setUserRole(jwtUser.getAuthorities().iterator().next().getAuthority());
            eventLog.setEventType("LOGOUT");
            eventLog.setDescription("Пользователь вышел из системы");
            eventLog.setResult("SUCCESS");
            eventLogService.save(eventLog);
        }
        response.getWriter().write("{\"message\":\"Выход выполнен\",\"data\":null}");
    }
}