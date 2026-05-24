package com.nester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestItem {
    private String materialId;
    private String materialName;
    private double quantity;
    private String unit;
}