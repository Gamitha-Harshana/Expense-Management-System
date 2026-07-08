package com.expensemanager.model;

import com.expensemanager.model.enums.ExpenseCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_limits")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BudgetLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Which manager-assigned pool this allocation draws from. Null = self-managed (legacy). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_budget_id")
    private com.expensemanager.model.EmployeeBudget employeeBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSpend = BigDecimal.ZERO;

    @NotNull
    @Column(nullable = false)
    private Integer month;

    @NotNull
    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private boolean alertSent = false;

    /**
     * Optional explicit start date (used for INDIVIDUAL self-managed budgets).
     * When set, this budget is active within [startDate, endDate].
     */
    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    /** True when today falls within [startDate, endDate] (individual) or always true when dates are null. */
    public boolean isActive() {
        if (startDate == null || endDate == null) return true;
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public double getUsagePercentage() {
        if (monthlyLimit.compareTo(BigDecimal.ZERO) == 0) return 0;
        return currentSpend.divide(monthlyLimit, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }
}
