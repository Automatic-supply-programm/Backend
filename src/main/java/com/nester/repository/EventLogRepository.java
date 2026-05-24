// EventLogRepository.java
package com.nester.repository;

import com.nester.model.EventLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository extends MongoRepository<EventLog, String> {
    List<EventLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<EventLog> findByUserId(String userId);
    List<EventLog> findByEventType(String eventType);

    @Query(value = "{}", fields = "{ 'eventType' : 1 }")
    List<EventLog> findAllEventTypes();
}