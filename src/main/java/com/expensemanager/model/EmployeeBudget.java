package com.expensemanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A budget pool that a Manager assigns to one Employee.
 * The employee then categorises portions of this pool using BudgetLimit records.
 */
@Entity
@Table(name = "employee_budgets")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class EmployeeBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The employee this budget belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    /** The manager who created this pool. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate endDate;

    /** Optional label, e.g. "July 2025" or "Conference Trip" */
    private String label;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ─────────────────────────────────────────────────

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public String getStatusLabel() {
        if (isExpired()) return "Expired";
        if (isActive())  return "Active";
        return "Upcoming";
    }
}
