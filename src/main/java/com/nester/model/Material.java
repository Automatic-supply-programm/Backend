package com.nester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "materials")
public class Material {
    @Id
    private String id;

    @NotBlank(message = "Артикул обязателен")
    @Indexed(unique = true)
    private String article;

    @NotBlank(message = "Наименование обязательно")
    private String name;

    @NotBlank(message = "Категория обязательна")
    private String category;

    @NotBlank(message = "Единица измерения обязательна")
    private String unit;

    @NotNull(message = "Текущий остаток обязателен")
    private double currentStock;

    @NotNull(message = "Критический остаток обязателен")
    private double criticalStock;

    @NotBlank(message = "Место хранения обязательно")
    private String storageLocation;

    private List<String> warehouses;
    private String description;
    private LocalDateTime lastReceiptDate;
    private LocalDateTime lastIssueDate;
    private List<MaterialBatch> batches = new ArrayList<>();
    private boolean archived = false;

    public String getStatus() {
        if (currentStock <= 0) return "OUT_OF_STOCK";
        if (currentStock <= criticalStock) return "CRITICAL";
        if (currentStock <= criticalStock * 1.5) return "LOW";
        return "NORMAL";
    }
}