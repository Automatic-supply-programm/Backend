package com.nester.repository;

import com.nester.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    @Query("{ $or: [ { senderId: ?0, receiverId: ?1 }, { senderId: ?1, receiverId: ?0 } ] }")
    List<Message> findConversation(String userId1, String userId2);

    long countByReceiverIdAndReadFalse(String receiverId);

    List<Message> findByReceiverIdAndSenderIdAndReadFalse(String receiverId, String senderId);

    @Query("{ receiverId: ?0, read: false }")
    List<Message> findUnreadByReceiverId(String receiverId);
}
