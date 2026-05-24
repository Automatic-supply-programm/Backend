package com.nester.service;

import com.nester.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    User findByLogin(String login);
    User findById(String id);
    List<User> findAll();
    User create(User user);
    User update(String id, User user);
    User delete(String id);
    User changeActive(String id, boolean active);
    long getActiveUsersCount();

    User updateLastLogin(String id, LocalDateTime lastLogin);

}