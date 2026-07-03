package com.expensemanager.controller;

import com.expensemanager.dto.ExpenseSearchDTO;
import com.expensemanager.model.Expense;
import com.expensemanager.service.ExportService;
import com.expensemanager.service.ExpenseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/export")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ExportController {

    private final ExportService exportService;
    private final ExpenseService expenseService;

    public ExportController(ExportService exportService, ExpenseService expenseService) {
        this.exportService = exportService;
        this.expenseService = expenseService;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(ExpenseSearchDTO searchDTO) {
        searchDTO.setSize(10000);
        List<Expense> expenses = expenseService.findAll(searchDTO).getContent();
        try {
            byte[] csv = exportService.exportToCsv(expenses);
            String filename = "expenses_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(ExpenseSearchDTO searchDTO) {
        searchDTO.setSize(10000);
        List<Expense> expenses = expenseService.findAll(searchDTO).getContent();
        try {
            byte[] pdf = exportService.exportToPdf(expenses);
            String filename = "expenses_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
