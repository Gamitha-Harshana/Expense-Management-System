package com.expensemanager.service;

import com.expensemanager.dto.BudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetService {
    BudgetLimit setLimit(BudgetDTO dto, User user);
    List<BudgetLimit> getLimitsForCurrentMonth(User user);
    List<BudgetLimit> getActiveLimitsForIndividual(User user);
    void updateSpending(User user, ExpenseCategory category, BigDecimal amount, LocalDate expenseDate);
    void checkAndSendBudgetAlerts();
    void deleteLimit(Long id, User user);
    void recalculateSpend(User user, ExpenseCategory category, LocalDate expenseDate);
}
