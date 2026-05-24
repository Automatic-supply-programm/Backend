// UserCreateRequest.java
package com.nester.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserCreateRequest {
    private String login;
    private String fullName;
    private String password;
    private String role;
    private String warehouseId;
    private List<String> productionLineIds;
    private List<String> managedWarehouseIds;
}