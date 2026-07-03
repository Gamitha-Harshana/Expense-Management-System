package com.expensemanager.controller;

import com.expensemanager.dto.EmployeeInviteDTO;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import com.expensemanager.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Manager Panel ─────────────────────────────────────────────
    @GetMapping("/manage")
    public String manage(@AuthenticationPrincipal UserDetails ud, Model model) {
        User manager = getCurrentUser(ud);
        List<Department> departments = departmentService.getDepartmentsForManager(manager);
        List<User> employees = departmentService.getEmployeesForManager(manager);
        model.addAttribute("departments", departments);
        model.addAttribute("employees", employees);
        model.addAttribute("employeeDTO", new EmployeeInviteDTO());
        model.addAttribute("pageTitle", "My Department");
        model.addAttribute("activePage", "department");
        return "department/manage";
    }

    // ── Invite Employee ───────────────────────────────────────────
    @PostMapping("/employees")
    public String inviteEmployee(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @ModelAttribute("employeeDTO") EmployeeInviteDTO dto,
            BindingResult result,
            RedirectAttributes ra) {
        User manager = getCurrentUser(ud);
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please fill in all required fields");
            return "redirect:/department/manage";
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            ra.addFlashAttribute("error", "Email already registered: " + dto.getEmail());
            return "redirect:/department/manage";
        }
        // Determine which department to use
        Department dept = null;
        if (dto.getDepartmentId() != null) {
            dept = companyService.findDepartmentById(dto.getDepartmentId()).orElse(null);
        } else {
            List<Department> depts = departmentService.getDepartmentsForManager(manager);
            if (!depts.isEmpty()) dept = depts.get(0);
        }
        User employee = departmentService.inviteEmployee(manager, dept, dto);
        String tempPwd = employee.getDepartmentName();
        employee.setDepartmentName(null);
        ra.addFlashAttribute("success",
                "Employee " + employee.getFullName() + " added. Temporary password: " + tempPwd);
        return "redirect:/department/manage";
    }

    // ── Remove Employee ───────────────────────────────────────────
    @PostMapping("/employees/{id}/remove")
    public String removeEmployee(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User manager = getCurrentUser(ud);
        userRepository.findById(id).ifPresent(employee -> {
            if (employee.getCompany() != null &&
                employee.getCompany().getId().equals(manager.getCompany().getId())) {
                departmentService.removeEmployee(employee);
            }
        });
        ra.addFlashAttribute("success", "Employee removed");
        return "redirect:/department/manage";
    }
}
