// User.java (уже есть)
package com.nester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String login;
    private String fullName;
    private String password;
    private String role; // ADMIN, WORKER, EMPLOYEE, MANAGER
    private boolean active = true;
    private LocalDateTime regDate = LocalDateTime.now();
    private LocalDateTime lastLogin;
    private String warehouseId;
    private List<String> productionLineIds;
    private List<String> managedWarehouseIds;
    private boolean deleted = false;
}