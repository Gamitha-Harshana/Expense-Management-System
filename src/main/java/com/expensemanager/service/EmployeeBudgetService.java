package com.expensemanager.service;

import com.expensemanager.dto.BudgetAllocationDTO;
import com.expensemanager.dto.EmployeeBudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeeBudgetService {

    /** Manager assigns a new budget pool to an employee. */
    EmployeeBudget assign(EmployeeBudgetDTO dto, User manager);

    /** All pools for a given employee (all time). */
    List<EmployeeBudget> getAllForEmployee(User employee);

    /** All pools assigned BY a manager. */
    List<EmployeeBudget> getAllAssignedByManager(User manager);

    /** Currently active pools for an employee. */
    List<EmployeeBudget> getActiveForEmployee(User employee);

    /** Sum of all allocations (BudgetLimit.monthlyLimit) tied to a pool. */
    BigDecimal getAllocatedAmount(EmployeeBudget pool);

    /** Remaining amount the employee can still allocate in this pool. */
    BigDecimal getRemainingAmount(EmployeeBudget pool);

    /** Employee allocates a portion of a pool into a category. */
    BudgetLimit allocate(BudgetAllocationDTO dto, User employee);

    /** Find pool by id (safe — checks employee ownership). */
    Optional<EmployeeBudget> findById(Long id);

    /** Delete a pool (manager only). */
    void delete(Long id, User manager);
}
