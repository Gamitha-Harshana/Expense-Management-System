package com.expensemanager.controller;

import com.expensemanager.dto.ExpenseSearchDTO;
import com.expensemanager.dto.ReportDTO;
import com.expensemanager.model.ExpenseReport;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.service.ExpenseService;
import com.expensemanager.service.ReportService;
import com.expensemanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final ExpenseService expenseService;

    public ReportController(ReportService reportService,
                            UserService userService,
                            ExpenseService expenseService) {
        this.reportService = reportService;
        this.userService = userService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public String listReports(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Page<ExpenseReport> reports = reportService.findAll(page, size);
        model.addAttribute("reports", reports);
        model.addAttribute("currentUser", userService.getCurrentUser());
        return "reports/list";
    }

    @GetMapping("/new")
    public String newReportForm(Model model) {
        model.addAttribute("reportDTO", new ReportDTO());
        ExpenseSearchDTO search = new ExpenseSearchDTO();
        search.setStatus(ExpenseStatus.APPROVED);
        search.setSize(100);
        model.addAttribute("approvedExpenses", expenseService.findAll(search));
        return "reports/form";
    }

    @PostMapping
    public String createReport(@Valid @ModelAttribute("reportDTO") ReportDTO dto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            ExpenseSearchDTO search = new ExpenseSearchDTO();
            search.setStatus(ExpenseStatus.APPROVED);
            search.setSize(100);
            model.addAttribute("approvedExpenses", expenseService.findAll(search));
            return "reports/form";
        }
        User currentUser = userService.getCurrentUser();
        ExpenseReport report = reportService.create(dto, currentUser);
        redirectAttributes.addFlashAttribute("success", "Report created successfully!");
        return "redirect:/reports/" + report.getId();
    }

    @GetMapping("/{id}")
    public String viewReport(@PathVariable Long id, Model model) {
        model.addAttribute("report", reportService.findById(id));
        model.addAttribute("currentUser", userService.getCurrentUser());
        return "reports/detail";
    }

    @PostMapping("/{id}/submit")
    public String submitReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reportService.submit(id, userService.getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Report submitted!");
        return "redirect:/reports/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reportService.approve(id, userService.getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Report approved!");
        return "redirect:/reports/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reportService.reject(id, userService.getCurrentUser());
        redirectAttributes.addFlashAttribute("warning", "Report rejected.");
        return "redirect:/reports/" + id;
    }

    @DeleteMapping("/{id}")
    public String deleteReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reportService.softDelete(id, userService.getCurrentUser());
        redirectAttributes.addFlashAttribute("success", "Report deleted.");
        return "redirect:/reports";
    }
}
