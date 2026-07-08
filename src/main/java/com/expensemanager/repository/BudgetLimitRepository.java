package com.expensemanager.repository;

import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {
    Optional<BudgetLimit> findByUserAndCategoryAndMonthAndYear(
            User user, ExpenseCategory category, int month, int year);
    List<BudgetLimit> findByUserAndMonthAndYear(User user, int month, int year);
    List<BudgetLimit> findByUserAndAlertSentFalse(User user);
}
