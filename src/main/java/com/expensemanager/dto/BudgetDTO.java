package com.expensemanager.dto;

import com.expensemanager.model.enums.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class BudgetDTO {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.01", message = "Limit must be greater than 0")
    private BigDecimal monthlyLimit;

    private Integer month;
    private Integer year;

    /** Optional: explicit start date for individual date-range budgets. */
    private LocalDate startDate;

    /** Optional: explicit end date for individual date-range budgets. */
    private LocalDate endDate;
}
