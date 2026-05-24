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
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/event-logs")
@RequiredArgsConstructor
public class EventLogController {
    private final EventLogService eventLogService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public void getAll(@RequestParam(required = false) String userId,
                       @RequestParam(required = false) String eventType,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<EventLog> events = eventLogService.findAllWithFilters(userId, eventType, startDate, endDate);
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, events));
    }

    @GetMapping("/types")
    public void getEventTypes(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        List<String> types = eventLogService.findAllEventTypes();
        objectMapper.writeValue(response.getWriter(), new ResponseResult<>(null, types));
    }
}