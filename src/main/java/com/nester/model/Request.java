// Request.java
package com.nester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "requests")
public class Request {
    @Id
    private String id;

    @Indexed(unique = true)
    private String number;

    private String type;        // ISSUE, REPLENISHMENT, RECEIPT, RETURN

    // Статусы:
    // ISSUE: UNDER_CONSIDERATION, APPROVED, REJECTED, CANCELLED, WAITING_CONFIRMATION, CONFIRMED
    // REPLENISHMENT/RECEIPT/RETURN: UNDER_CONSIDERATION, APPROVED, REJECTED, SENT_FOR_REVISION, CANCELLED
    private String status;

    private LocalDateTime createdDate = LocalDateTime.now();

    private String requesterId;
    private String requesterName;
    private String requesterRole;

    private String sourceId;
    private String sourceName;      // склад или производственный участок

    private String destinationId;
    private String destinationName;  // работник склада или менеджер

    private List<RequestItem> items = new ArrayList<>();

    private String comment;
    private boolean archived = false;
}