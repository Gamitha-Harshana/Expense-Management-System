package com.expensemanager.service;

import com.expensemanager.dto.ExpenseDTO;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.*;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.impl.ExpenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private BudgetService budgetService;

    @InjectMocks private ExpenseServiceImpl expenseService;

    private User testUser;
    private User managerUser;
    private Expense testExpense;
    private ExpenseDTO testDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john@test.com");
        testUser.setRole(Role.ROLE_EMPLOYEE);

        managerUser = new User();
        managerUser.setId(2L);
        managerUser.setFirstName("Jane");
        managerUser.setLastName("Smith");
        managerUser.setEmail("jane@test.com");
        managerUser.setRole(Role.ROLE_MANAGER);

        testExpense = new Expense();
        testExpense.setId(10L);
        testExpense.setTitle("Business Lunch");
        testExpense.setAmount(new BigDecimal("75.00"));
        testExpense.setCurrency("USD");
        testExpense.setCategory(ExpenseCategory.MEALS);
        testExpense.setExpenseType(ExpenseType.CORPORATE);
        testExpense.setStatus(ExpenseStatus.DRAFT);
        testExpense.setExpenseDate(LocalDate.now());
        testExpense.setUser(testUser);

        testDTO = new ExpenseDTO();
        testDTO.setTitle("Business Lunch");
        testDTO.setAmount(new BigDecimal("75.00"));
        testDTO.setCategory(ExpenseCategory.MEALS);
        testDTO.setExpenseType(ExpenseType.CORPORATE);
        testDTO.setExpenseDate(LocalDate.now());
    }

    @Test
    @DisplayName("Create expense — should save and return expense")
    void createExpense_ShouldSaveExpense() {
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        Expense result = expenseService.create(testDTO, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Business Lunch");
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("Find expense by ID — should return expense when found")
    void findById_ShouldReturnExpense_WhenExists() {
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));

        Expense result = expenseService.findById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Find expense by ID — should throw when not found")
    void findById_ShouldThrow_WhenNotFound() {
        when(expenseRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Submit expense — should change status to SUBMITTED")
    void submit_ShouldChangeStatus_WhenDraft() {
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(userRepository.findActiveByRole(Role.ROLE_MANAGER)).thenReturn(List.of(managerUser));

        expenseService.submit(10L, testUser);

        assertThat(testExpense.getStatus()).isEqualTo(ExpenseStatus.SUBMITTED);
        assertThat(testExpense.getSubmittedAt()).isNotNull();
        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Submit — should throw when expense not in DRAFT")
    void submit_ShouldThrow_WhenNotDraft() {
        testExpense.setStatus(ExpenseStatus.SUBMITTED);
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));

        assertThatThrownBy(() -> expenseService.submit(10L, testUser))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Approve expense — should change status to APPROVED")
    void approve_ShouldChangeStatus() {
        testExpense.setStatus(ExpenseStatus.SUBMITTED);
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        expenseService.approve(10L, managerUser);

        assertThat(testExpense.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        assertThat(testExpense.getApprover()).isEqualTo(managerUser);
        assertThat(testExpense.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("Reject expense — should change status to REJECTED with reason")
    void reject_ShouldSetRejectionReason() {
        testExpense.setStatus(ExpenseStatus.SUBMITTED);
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        expenseService.reject(10L, managerUser, "Missing receipt");

        assertThat(testExpense.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
        assertThat(testExpense.getRejectionReason()).isEqualTo("Missing receipt");
    }

    @Test
    @DisplayName("Soft delete — should set deletedAt")
    void softDelete_ShouldSetDeletedAt() {
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        expenseService.softDelete(10L, testUser);

        assertThat(testExpense.getDeletedAt()).isNotNull();
        verify(expenseRepository, times(1)).save(testExpense);
    }

    @Test
    @DisplayName("Soft delete — should throw when user is not owner")
    void softDelete_ShouldThrow_WhenNotOwner() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setRole(Role.ROLE_EMPLOYEE);
        when(expenseRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(testExpense));

        assertThatThrownBy(() -> expenseService.softDelete(10L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }
}
