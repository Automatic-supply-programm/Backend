package com.nester.service;

import com.nester.dto.MessageDTO;

import java.util.List;
import java.util.Map;

public interface MessageService {
    MessageDTO sendMessage(String senderId, String receiverId, String content);
    List<MessageDTO> getConversation(String userId1, String userId2);
    long getUnreadCount(String userId);
    Map<String, Long> getUnreadCountBySender(String userId);
    void markAsRead(String receiverId, String senderId);
}
