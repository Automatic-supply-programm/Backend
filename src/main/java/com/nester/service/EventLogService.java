// EventLogService.java
package com.nester.service;

import com.nester.model.EventLog;
import java.time.LocalDateTime;
import java.util.List;

public interface EventLogService {
    EventLog save(EventLog eventLog);
    List<EventLog> findAllWithFilters(String userId, String eventType, String startDate, String endDate);
    List<String> findAllEventTypes();
}