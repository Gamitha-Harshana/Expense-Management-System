package com.expensemanager.service;

import com.expensemanager.dto.UserRegistrationDTO;
import com.expensemanager.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(UserRegistrationDTO dto);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> findAllActive();
    List<User> findAllDeleted();
    User getCurrentUser();
    void softDelete(Long id);
    void restore(Long id);
    boolean existsByEmail(String email);
}
