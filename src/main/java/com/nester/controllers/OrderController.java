package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.Order;
import com.nester.repository.OrderRepository;
import com.nester.security.jwt.JwtUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public void getAll(@RequestParam(defaultValue = "false") boolean all,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<Order> orders = all ? orderRepository.findAll() : orderRepository.findByActive(true);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, orders));
    }

    @PostMapping
    public void create(@RequestBody Order order, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        String role = ((JwtUser) auth.getPrincipal()).getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_MANAGER") && !role.equals("ROLE_WORKER")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }
        order.setCreatedDate(LocalDateTime.now());
        order.setActive(true);
        Order saved = orderRepository.save(order);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, saved));
    }

    @PutMapping("/{id}")
    public void update(@PathVariable String id, @RequestBody Order order,
                       Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        String role = ((JwtUser) auth.getPrincipal()).getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_MANAGER")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден"));
        existing.setNumber(order.getNumber());
        existing.setDescription(order.getDescription());
        existing.setActive(order.isActive());
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, orderRepository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id, Authentication auth, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        String role = ((JwtUser) auth.getPrincipal()).getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN")) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            objectMapper.writeValue(response.getWriter(), new ResponseResult<>("Недостаточно прав", null));
            return;
        }
        orderRepository.deleteById(id);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, null));
    }
}
