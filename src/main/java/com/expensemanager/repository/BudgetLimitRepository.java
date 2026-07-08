package com.expensemanager.repository;

import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {

    Optional<BudgetLimit> findByUserAndCategoryAndMonthAndYear(
            User user, ExpenseCategory category, int month, int year);

    List<BudgetLimit> findByUserAndMonthAndYear(User user, int month, int year);

    List<BudgetLimit> findByUserAndAlertSentFalse(User user);

    // Pool-based queries
    List<BudgetLimit> findByEmployeeBudget(EmployeeBudget pool);

    Optional<BudgetLimit> findByUserAndCategoryAndEmployeeBudget(
            User user, ExpenseCategory category, EmployeeBudget pool);

    /** Active individual budgets: today is within [startDate, endDate]. */
    @Query("""
        SELECT b FROM BudgetLimit b
        WHERE b.user = :user
          AND b.startDate IS NOT NULL
          AND b.startDate <= :today
          AND b.endDate   >= :today
        ORDER BY b.startDate DESC
        """)
    List<BudgetLimit> findActiveByUserDateRange(
            @Param("user") User user,
            @Param("today") LocalDate today);

    void deleteByEmployeeBudget(EmployeeBudget pool);
}
