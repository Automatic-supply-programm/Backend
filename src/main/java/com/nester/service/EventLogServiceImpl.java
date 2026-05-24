// EventLogServiceImpl.java
package com.nester.service;

import com.nester.model.EventLog;
import com.nester.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventLogServiceImpl implements EventLogService {
    private final EventLogRepository eventLogRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public EventLog save(EventLog eventLog) {
        return eventLogRepository.save(eventLog);
    }

    @Override
    public List<EventLog> findAllWithFilters(String userId, String eventType, String startDate, String endDate) {
        List<EventLog> events = eventLogRepository.findAll();

        if (userId != null && !userId.isEmpty()) {
            events = events.stream()
                    .filter(e -> userId.equals(e.getUserId()))
                    .collect(Collectors.toList());
        }

        if (eventType != null && !eventType.isEmpty()) {
            events = events.stream()
                    .filter(e -> eventType.equals(e.getEventType()))
                    .collect(Collectors.toList());
        }

        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
            events = events.stream()
                    .filter(e -> e.getTimestamp().isAfter(start) || e.getTimestamp().isEqual(start))
                    .collect(Collectors.toList());
        }

        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate, DATE_FORMATTER).atTime(23, 59, 59);
            events = events.stream()
                    .filter(e -> e.getTimestamp().isBefore(end) || e.getTimestamp().isEqual(end))
                    .collect(Collectors.toList());
        }

        events.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return events;
    }

    @Override
    public List<String> findAllEventTypes() {
        return eventLogRepository.findAllEventTypes().stream()
                .map(EventLog::getEventType)
                .distinct()
                .collect(Collectors.toList());
    }
}