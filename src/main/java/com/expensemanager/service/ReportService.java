package com.expensemanager.service;

import com.expensemanager.dto.ReportDTO;
import com.expensemanager.model.ExpenseReport;
import com.expensemanager.model.User;
import org.springframework.data.domain.Page;

public interface ReportService {
    ExpenseReport create(ReportDTO dto, User user);
    ExpenseReport findById(Long id);
    Page<ExpenseReport> findAll(int page, int size);
    void submit(Long id, User user);
    void approve(Long id, User approver);
    void reject(Long id, User approver);
    void softDelete(Long id, User user);
}
