package com.expensemanager.service;

import com.expensemanager.dto.BudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.BudgetLimitRepository;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.service.impl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Unit Tests")
class BudgetServiceTest {

    @Mock private BudgetLimitRepository budgetLimitRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    @InjectMocks private BudgetServiceImpl budgetService;

    private User testUser;
    private BudgetDTO budgetDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.ROLE_INDIVIDUAL);

        budgetDTO = new BudgetDTO();
        budgetDTO.setCategory(ExpenseCategory.MEALS);
        budgetDTO.setMonthlyLimit(new BigDecimal("500.00"));
        budgetDTO.setMonth(LocalDate.now().getMonthValue());
        budgetDTO.setYear(LocalDate.now().getYear());
    }

    @Test
    @DisplayName("Set budget — should create new budget limit")
    void setLimit_ShouldCreateNewBudget() {
        when(budgetLimitRepository.findByUserAndCategoryAndMonthAndYear(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(expenseRepository.sumByUserCategoryMonthYear(any(), any(), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);

        BudgetLimit limit = new BudgetLimit();
        limit.setMonthlyLimit(new BigDecimal("500.00"));
        limit.setCategory(ExpenseCategory.MEALS);
        when(budgetLimitRepository.save(any(BudgetLimit.class))).thenReturn(limit);

        BudgetLimit result = budgetService.setLimit(budgetDTO, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getMonthlyLimit()).isEqualByComparingTo("500.00");
        verify(budgetLimitRepository).save(any(BudgetLimit.class));
    }

    @Test
    @DisplayName("Set budget — should update existing budget limit")
    void setLimit_ShouldUpdateExistingBudget() {
        BudgetLimit existing = new BudgetLimit();
        existing.setCategory(ExpenseCategory.MEALS);
        existing.setMonthlyLimit(new BigDecimal("300.00"));
        existing.setCurrentSpend(BigDecimal.ZERO);

        when(budgetLimitRepository.findByUserAndCategoryAndMonthAndYear(
                any(), any(), any(), any())).thenReturn(Optional.of(existing));
        when(expenseRepository.sumByUserCategoryMonthYear(any(), any(), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(budgetLimitRepository.save(any(BudgetLimit.class))).thenReturn(existing);

        BudgetLimit result = budgetService.setLimit(budgetDTO, testUser);

        assertThat(result.getMonthlyLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Update spending — should trigger alert at 80% usage")
    void updateSpending_ShouldSendAlert_WhenOver80Percent() {
        BudgetLimit limit = new BudgetLimit();
        limit.setCategory(ExpenseCategory.MEALS);
        limit.setMonthlyLimit(new BigDecimal("100.00"));
        limit.setCurrentSpend(new BigDecimal("75.00"));
        limit.setAlertSent(false);
        limit.setUser(testUser);

        when(budgetLimitRepository.findByUserAndCategoryAndMonthAndYear(
                any(), any(), any(), any())).thenReturn(Optional.of(limit));
        when(budgetLimitRepository.save(any())).thenReturn(limit);

        // Adding 10 makes it 85% — should trigger alert
        budgetService.updateSpending(testUser, ExpenseCategory.MEALS,
                new BigDecimal("10.00"), LocalDate.now());

        verify(notificationService, times(1)).create(any(), any(), any(), any(), any());
        verify(emailService, times(1)).sendBudgetWarningNotification(any(), any(), anyDouble());
    }
}
