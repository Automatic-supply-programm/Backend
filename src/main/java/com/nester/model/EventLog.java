package com.nester.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "event_logs")
public class EventLog {
    @Id
    private String id;
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    private LocalDateTime timestamp;
    private String userId;
    private String userFullName;
    private String userRole;
    private String eventType;
    private String objectType;
    private String objectId;
    private String objectNumber;
    private String description;
    private String result;
    // дополнительные поля для фильтрации менеджером
    private String warehouseId;      // ID склада, к которому относится событие
    private String productionLineId; // ID участка, если применимо
}