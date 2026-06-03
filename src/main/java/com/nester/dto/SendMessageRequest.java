package com.nester.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String receiverId;
    private String content;
}
