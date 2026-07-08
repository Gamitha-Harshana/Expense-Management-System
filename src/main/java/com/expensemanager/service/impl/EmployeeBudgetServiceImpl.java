package com.expensemanager.service.impl;

import com.expensemanager.dto.BudgetAllocationDTO;
import com.expensemanager.dto.EmployeeBudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;
import com.expensemanager.repository.BudgetLimitRepository;
import com.expensemanager.repository.EmployeeBudgetRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.EmployeeBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeBudgetServiceImpl implements EmployeeBudgetService {

    private final EmployeeBudgetRepository employeeBudgetRepository;
    private final BudgetLimitRepository    budgetLimitRepository;
    private final UserRepository           userRepository;

    @Override
    public EmployeeBudget assign(EmployeeBudgetDTO dto, User manager) {
        User employee = userRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        EmployeeBudget pool = new EmployeeBudget();
        pool.setEmployee(employee);
        pool.setAssignedBy(manager);
        pool.setTotalAmount(dto.getTotalAmount());
        pool.setStartDate(dto.getStartDate());
        pool.setEndDate(dto.getEndDate());
        pool.setLabel(dto.getLabel() != null && !dto.getLabel().isBlank()
                ? dto.getLabel()
                : dto.getStartDate() + " – " + dto.getEndDate());
        return employeeBudgetRepository.save(pool);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeBudget> getAllForEmployee(User employee) {
        return employeeBudgetRepository.findByEmployeeOrderByStartDateDesc(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeBudget> getAllAssignedByManager(User manager) {
        return employeeBudgetRepository.findByAssignedByOrderByStartDateDesc(manager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeBudget> getActiveForEmployee(User employee) {
        return employeeBudgetRepository.findActiveByEmployee(employee, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAllocatedAmount(EmployeeBudget pool) {
        return budgetLimitRepository.findByEmployeeBudget(pool)
                .stream()
                .map(BudgetLimit::getMonthlyLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingAmount(EmployeeBudget pool) {
        return pool.getTotalAmount().subtract(getAllocatedAmount(pool));
    }

    @Override
    public BudgetLimit allocate(BudgetAllocationDTO dto, User employee) {
        EmployeeBudget pool = employeeBudgetRepository.findById(dto.getEmployeeBudgetId())
                .orElseThrow(() -> new IllegalArgumentException("Budget pool not found"));

        if (!pool.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalArgumentException("This budget pool does not belong to you");
        }

        BigDecimal remaining = getRemainingAmount(pool);
        if (dto.getAmount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                "Amount exceeds remaining pool balance of " + remaining);
        }

        // Check if an allocation for this category already exists in this pool
        BudgetLimit limit = budgetLimitRepository
                .findByUserAndCategoryAndEmployeeBudget(employee, dto.getCategory(), pool)
                .orElseGet(() -> {
                    BudgetLimit l = new BudgetLimit();
                    l.setUser(employee);
                    l.setCategory(dto.getCategory());
                    l.setEmployeeBudget(pool);
                    // Use pool dates for month/year (start month)
                    l.setMonth(pool.getStartDate().getMonthValue());
                    l.setYear(pool.getStartDate().getYear());
                    return l;
                });

        limit.setMonthlyLimit(dto.getAmount());
        return budgetLimitRepository.save(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeBudget> findById(Long id) {
        return employeeBudgetRepository.findById(id);
    }

    @Override
    public void delete(Long id, User manager) {
        employeeBudgetRepository.findById(id).ifPresent(pool -> {
            if (pool.getAssignedBy().getId().equals(manager.getId())) {
                // Remove allocations first
                budgetLimitRepository.deleteByEmployeeBudget(pool);
                employeeBudgetRepository.delete(pool);
            }
        });
    }
}
