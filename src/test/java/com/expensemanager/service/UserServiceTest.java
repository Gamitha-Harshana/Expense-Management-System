package com.expensemanager.service;

import com.expensemanager.dto.UserRegistrationDTO;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private UserRegistrationDTO registrationDTO;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registrationDTO = new UserRegistrationDTO();
        registrationDTO.setFirstName("Alice");
        registrationDTO.setLastName("Brown");
        registrationDTO.setEmail("alice@test.com");
        registrationDTO.setPassword("password123");
        registrationDTO.setConfirmPassword("password123");
        registrationDTO.setRole(Role.ROLE_INDIVIDUAL);

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFirstName("Alice");
        savedUser.setLastName("Brown");
        savedUser.setEmail("alice@test.com");
        savedUser.setRole(Role.ROLE_INDIVIDUAL);
    }

    @Test
    @DisplayName("Register — should create user successfully")
    void register_ShouldCreateUser() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(registrationDTO);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Alice");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Register — should throw when email already exists")
    void register_ShouldThrow_WhenEmailExists() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register — should throw when passwords do not match")
    void register_ShouldThrow_WhenPasswordMismatch() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        registrationDTO.setConfirmPassword("different");

        assertThatThrownBy(() -> userService.register(registrationDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    @DisplayName("Find by email — should return user when found")
    void findByEmail_ShouldReturn_WhenFound() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(savedUser));

        Optional<User> result = userService.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("Exists by email — should return true when user exists")
    void existsByEmail_ShouldReturnTrue() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);
        assertThat(userService.existsByEmail("alice@test.com")).isTrue();
    }
}
