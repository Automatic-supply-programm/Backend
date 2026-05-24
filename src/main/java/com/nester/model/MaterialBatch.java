// MaterialBatch.java
package com.nester.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialBatch {
    private String batchNumber;
    private LocalDateTime receiptDate;
    private double initialQuantity;
    private double currentQuantity;
    private String storageLocation;
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")

    private LocalDateTime expiryDate;
    private String receiptActNumber;
    private String acceptedByUserId;
    private String acceptedByName;
    private String confirmedByUserId;
    private String confirmedByName;
}