package com.nester.service;

import com.nester.dto.MessageDTO;
import com.nester.model.Message;
import com.nester.model.User;
import com.nester.repository.MessageRepository;
import com.nester.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public MessageDTO sendMessage(String senderId, String receiverId, String content) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        Message message = new Message();
        message.setSenderId(senderId);
        message.setSenderName(sender.getFullName());
        message.setSenderLogin(sender.getLogin());
        message.setReceiverId(receiverId);
        message.setReceiverName(receiver.getFullName());
        message.setReceiverLogin(receiver.getLogin());
        message.setContent(content.trim());
        message.setCreatedAt(LocalDateTime.now());
        message.setRead(false);

        Message saved = messageRepository.save(message);
        MessageDTO dto = toDTO(saved);

        // Доставка получателю и эхо отправителю — оба видят сообщение в реальном времени
        messagingTemplate.convertAndSendToUser(receiver.getLogin(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(sender.getLogin(), "/queue/messages", dto);

        return dto;
    }

    @Override
    public List<MessageDTO> getConversation(String userId1, String userId2) {
        return messageRepository.findConversation(userId1, userId2).stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String userId) {
        return messageRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Override
    public Map<String, Long> getUnreadCountBySender(String userId) {
        return messageRepository.findUnreadByReceiverId(userId).stream()
                .collect(Collectors.groupingBy(Message::getSenderId, Collectors.counting()));
    }

    @Override
    public void markAsRead(String receiverId, String senderId) {
        List<Message> unread = messageRepository.findByReceiverIdAndSenderIdAndReadFalse(receiverId, senderId);
        unread.forEach(m -> m.setRead(true));
        messageRepository.saveAll(unread);
    }

    private MessageDTO toDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setSenderId(m.getSenderId());
        dto.setSenderName(m.getSenderName());
        dto.setReceiverId(m.getReceiverId());
        dto.setReceiverName(m.getReceiverName());
        dto.setContent(m.getContent());
        dto.setCreatedAt(m.getCreatedAt());
        dto.setRead(m.isRead());
        return dto;
    }
}
