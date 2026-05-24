// LoginRequest.java
package com.nester.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String login;
    private String password;
}