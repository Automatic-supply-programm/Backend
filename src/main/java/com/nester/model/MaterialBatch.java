// MaterialBatch.java
package com.nester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
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
    private LocalDate expiryDate;   // только дата, без времени
    private String receiptActNumber;
    private String acceptedByUserId;
    private String acceptedByName;
    private String confirmedByUserId;
    private String confirmedByName;
}