// EventLogController.java
package com.nester.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nester.dto.ResponseResult;
import com.nester.model.EventLog;
import com.nester.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/event-logs")
@RequiredArgsConstructor
public class EventLogController {
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public void getAll(@RequestParam(required = false) String userId,
                       @RequestParam(required = false) String userRole,
                       @RequestParam(required = false) String eventType,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String search,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<EventLog> events = eventLogService.findAllWithFilters(userId, eventType, startDate, endDate);

        if (userRole != null && !userRole.isEmpty()) {
            events = events.stream()
                    .filter(e -> userRole.equals(e.getUserRole()))
                    .collect(Collectors.toList());
        }

        // Поиск по пользователю, описанию, номеру заявки, артикулу/objectNumber
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            events = events.stream()
                    .filter(e ->
                            (e.getUserFullName() != null && e.getUserFullName().toLowerCase().contains(q)) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(q)) ||
                            (e.getObjectNumber() != null && e.getObjectNumber().toLowerCase().contains(q)) ||
                            (e.getObjectId() != null && e.getObjectId().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, events));
    }

    @GetMapping("/types")
    public void getEventTypes(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<String> types = eventLogService.findAllEventTypes();
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, types));
    }
}
