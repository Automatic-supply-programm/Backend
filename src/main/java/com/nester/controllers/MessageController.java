package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.MessageDTO;
import com.nester.dto.ResponseResult;
import com.nester.dto.SendMessageRequest;
import com.nester.security.jwt.JwtUser;
import com.nester.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/conversation/{userId}")
    public void getConversation(@PathVariable String userId, Authentication auth,
                                HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        List<MessageDTO> messages = messageService.getConversation(jwtUser.getId(), userId);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, messages));
    }

    @GetMapping("/unread/count")
    public void getUnreadCount(Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        long total = messageService.getUnreadCount(jwtUser.getId());
        Map<String, Long> bySender = messageService.getUnreadCountBySender(jwtUser.getId());
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null,
                Map.of("total", total, "bySender", bySender)));
    }

    @PostMapping("/read/{senderId}")
    public void markAsRead(@PathVariable String senderId, Authentication auth,
                           HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        messageService.markAsRead(jwtUser.getId(), senderId);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, "OK"));
    }

    // WebSocket handler: клиент шлёт на /app/chat
    @MessageMapping("/chat")
    public void handleChatMessage(@Payload SendMessageRequest request, Principal principal) {
        JwtUser jwtUser = (JwtUser) ((Authentication) principal).getPrincipal();
        messageService.sendMessage(jwtUser.getId(), request.getReceiverId(), request.getContent());
    }
}
