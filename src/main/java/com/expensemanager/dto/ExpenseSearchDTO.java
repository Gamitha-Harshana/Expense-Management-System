package com.expensemanager.dto;

import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.model.enums.ExpenseType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExpenseSearchDTO {
    private String keyword;
    private ExpenseCategory category;
    private ExpenseStatus status;
    private ExpenseType expenseType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
    private int page = 0;
    private int size = 10;
}
