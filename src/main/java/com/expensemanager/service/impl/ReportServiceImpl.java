package com.expensemanager.service.impl;

import com.expensemanager.dto.ReportDTO;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.model.Expense;
import com.expensemanager.model.ExpenseReport;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.repository.ExpenseReportRepository;
import com.expensemanager.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ExpenseReportRepository reportRepository;
    private final ExpenseRepository expenseRepository;

    public ReportServiceImpl(ExpenseReportRepository reportRepository,
                             ExpenseRepository expenseRepository) {
        this.reportRepository = reportRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public ExpenseReport create(ReportDTO dto, User user) {
        ExpenseReport report = new ExpenseReport();
        report.setName(dto.getName());
        report.setDescription(dto.getDescription());
        report.setStartDate(dto.getStartDate());
        report.setEndDate(dto.getEndDate());
        report.setSubmittedBy(user);
        report.setDepartment(user.getDepartmentName() != null ? user.getDepartmentName() :
                (user.getDepartment() != null ? user.getDepartment().getName() : null));
        report.setStatus(ExpenseStatus.DRAFT);

        if (dto.getExpenseIds() != null && !dto.getExpenseIds().isEmpty()) {
            List<Expense> expenses = expenseRepository.findAllById(dto.getExpenseIds());
            expenses.forEach(e -> e.setReport(report));
            report.setExpenses(expenses);
            report.recalculateTotal();
        }

        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseReport findById(Long id) {
        return reportRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseReport> findAll(int page, int size) {
        return reportRepository.findAllActive(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Override
    public void submit(Long id, User user) {
        ExpenseReport report = findById(id);
        if (!report.getSubmittedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your report");
        }
        report.setStatus(ExpenseStatus.SUBMITTED);
        reportRepository.save(report);
    }

    @Override
    public void approve(Long id, User approver) {
        ExpenseReport report = findById(id);
        report.setStatus(ExpenseStatus.APPROVED);
        report.setApprover(approver);
        reportRepository.save(report);
    }

    @Override
    public void reject(Long id, User approver) {
        ExpenseReport report = findById(id);
        report.setStatus(ExpenseStatus.REJECTED);
        report.setApprover(approver);
        reportRepository.save(report);
    }

    @Override
    public void softDelete(Long id, User user) {
        ExpenseReport report = findById(id);
        report.setDeletedAt(LocalDateTime.now());
        reportRepository.save(report);
    }
}
