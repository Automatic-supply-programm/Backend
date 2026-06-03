package com.nester.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String senderId;
    private String senderName;
    private String senderLogin;
    private String receiverId;
    private String receiverName;
    private String receiverLogin;
    private String content;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean read = false;
}
