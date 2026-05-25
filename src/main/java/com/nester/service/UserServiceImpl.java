package com.nester.service;

import com.nester.model.User;
import com.nester.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Логин уже занят");
        }
    }

    @Override
    public User update(String id, User user) {
        User existing = findById(id);
        if (user.getLogin() != null && !user.getLogin().isEmpty()) {
            existing.setLogin(user.getLogin());
        }
        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            existing.setFullName(user.getFullName());
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            existing.setRole(user.getRole());
        }
        if (user.getWarehouseId() != null) {
            existing.setWarehouseId(user.getWarehouseId());
        }
        if (user.getProductionLineIds() != null) {
            existing.setProductionLineIds(user.getProductionLineIds());
        }
        if (user.getManagedWarehouseIds() != null) {
            existing.setManagedWarehouseIds(user.getManagedWarehouseIds());
        }
        return userRepository.save(existing);
    }

    @Override
    public User delete(String id) {
        User user = findById(id);
        user.setDeleted(true);          // soft delete
        return userRepository.save(user);
    }

    @Override
    public User changeActive(String id, boolean active) {
        User user = findById(id);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Override
    public long getActiveUsersCount() {
        return userRepository.countByActive(true);
    }

    @Override
    public User updateLastLogin(String id, LocalDateTime lastLogin) {
        User user = findById(id);
        user.setLastLogin(lastLogin);
        return userRepository.save(user);
    }
}