// EventLogService.java
package com.nester.service;

import com.nester.model.EventLog;
import java.util.List;
import java.util.Set;

public interface EventLogService {
    // Системные события — видны только ADMIN (входы, управление пользователями)
    Set<String> SYSTEM_EVENT_TYPES = Set.of(
            "LOGIN", "LOGOUT", "LOGIN_FAILED",
            "USER_CREATED", "USER_UPDATED", "USER_DELETED",
            "ACCESS_SUSPENDED", "ACCESS_RESTORED", "PASSWORD_CHANGED"
    );

    EventLog save(EventLog eventLog);
    List<EventLog> findAllWithFilters(String userId, String eventType, String startDate, String endDate);
    List<String> findAllEventTypes();
}