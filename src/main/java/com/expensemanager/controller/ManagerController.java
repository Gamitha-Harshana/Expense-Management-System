package com.expensemanager.controller;

import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import com.expensemanager.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final DepartmentService departmentService;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Add Employee Form ────────────────────────────────────────────
    @GetMapping("/add-employee")
    public String addEmployeeForm(@AuthenticationPrincipal UserDetails ud, Model model) {
        User manager = getCurrentUser(ud);
        List<Department> departments = departmentService.getDepartmentsForManager(manager);
        model.addAttribute("departments", departments);
        model.addAttribute("currentUser", manager);
        model.addAttribute("pageTitle", "Add Employee");
        model.addAttribute("activePage", "department");
        return "manager/add-employee";
    }

    // ── Deactivate Employee ─────────────────────────────────────────
    @PostMapping("/employees/{id}/deactivate")
    public String deactivate(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User manager = getCurrentUser(ud);
        userRepository.findById(id).ifPresent(emp -> {
            if (emp.getCompany() != null &&
                emp.getCompany().getId().equals(manager.getCompany().getId())) {
                emp.setEnabled(false);
                userRepository.save(emp);
            }
        });
        ra.addFlashAttribute("success", "Employee deactivated");
        return "redirect:/department/manage";
    }

    // ── Activate Employee ───────────────────────────────────────────
    @PostMapping("/employees/{id}/activate")
    public String activate(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User manager = getCurrentUser(ud);
        userRepository.findById(id).ifPresent(emp -> {
            if (emp.getCompany() != null &&
                emp.getCompany().getId().equals(manager.getCompany().getId())) {
                emp.setEnabled(true);
                userRepository.save(emp);
            }
        });
        ra.addFlashAttribute("success", "Employee activated");
        return "redirect:/department/manage";
    }
}
