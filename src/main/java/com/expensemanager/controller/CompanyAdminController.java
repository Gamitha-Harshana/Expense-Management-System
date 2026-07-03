package com.expensemanager.controller;

import com.expensemanager.dto.DepartmentDTO;
import com.expensemanager.dto.ManagerInviteDTO;
import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final CompanyService companyService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    private Company getCompany(User user) {
        return companyService.findByCeo(user)
                .orElseThrow(() -> new RuntimeException("No company found for CEO"));
    }

    // ── Company Overview ─────────────────────────────────────────
    @GetMapping("/admin")
    public String overview(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        model.addAttribute("company", company);
        model.addAttribute("departments", companyService.getDepartmentsByCompany(company));
        model.addAttribute("managers", userRepository.findManagersByCompany(company));
        model.addAttribute("departmentDTO", new DepartmentDTO());
        model.addAttribute("managerDTO", new ManagerInviteDTO());
        model.addAttribute("pageTitle", "Company Admin");
        model.addAttribute("activePage", "company");
        return "company/admin";
    }

    // ── Add Manager Form ──────────────────────────────────────────
    @GetMapping("/admin/add-manager")
    public String addManagerForm(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        model.addAttribute("departments", companyService.getDepartmentsByCompany(company));
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Add Manager");
        model.addAttribute("activePage", "company");
        return "company/add-manager";
    }

    // ── Add Department Form ───────────────────────────────────────
    @GetMapping("/admin/add-department")
    public String addDepartmentForm(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        model.addAttribute("managers", userRepository.findManagersByCompany(company));
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Add Department");
        model.addAttribute("activePage", "company");
        return "company/add-department";
    }

    // ── Create Department ─────────────────────────────────────────
    @PostMapping("/departments")
    public String createDepartment(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @ModelAttribute("departmentDTO") DepartmentDTO dto,
            BindingResult result,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Department name is required");
            return "redirect:/company/admin";
        }
        companyService.createDepartment(company, dto);
        ra.addFlashAttribute("success", "Department created successfully");
        return "redirect:/company/admin";
    }

    // ── Delete Department ─────────────────────────────────────────
    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        companyService.findDepartmentById(id).ifPresent(dept -> {
            if (dept.getCompany().getId().equals(company.getId())) {
                // just remove manager association, keep dept for now
                dept.setManager(null);
                companyService.save(company);
            }
        });
        ra.addFlashAttribute("success", "Department updated");
        return "redirect:/company/admin";
    }

    // ── Invite Manager ────────────────────────────────────────────
    @PostMapping("/managers")
    public String inviteManager(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @ModelAttribute("managerDTO") ManagerInviteDTO dto,
            BindingResult result,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Company company = getCompany(user);
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please fill in all required fields");
            return "redirect:/company/admin";
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            ra.addFlashAttribute("error", "Email already registered: " + dto.getEmail());
            return "redirect:/company/admin";
        }
        User manager = companyService.inviteManager(company, dto);
        String tempPwd = manager.getDepartmentName(); // temporary password stored here
        manager.setDepartmentName(null); // clear it
        ra.addFlashAttribute("success",
                "Manager " + manager.getFullName() + " added. Temporary password: " + tempPwd);
        return "redirect:/company/admin";
    }

    // ── Remove Manager ────────────────────────────────────────────
    @PostMapping("/managers/{id}/remove")
    public String removeManager(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User ceo = getCurrentUser(ud);
        Company company = getCompany(ceo);
        userRepository.findById(id).ifPresent(manager -> {
            if (manager.getCompany() != null &&
                manager.getCompany().getId().equals(company.getId())) {
                companyService.removeManager(manager);
            }
        });
        ra.addFlashAttribute("success", "Manager removed");
        return "redirect:/company/admin";
    }

    // ── Assign Manager to Department ──────────────────────────────
    @PostMapping("/departments/{deptId}/assign-manager")
    public String assignManager(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long deptId,
            @RequestParam Long managerId,
            RedirectAttributes ra) {
        User ceo = getCurrentUser(ud);
        Company company = getCompany(ceo);
        companyService.findDepartmentById(deptId).ifPresent(dept -> {
            if (dept.getCompany().getId().equals(company.getId())) {
                userRepository.findById(managerId).ifPresent(manager ->
                        companyService.assignManagerToDepartment(dept, manager));
            }
        });
        ra.addFlashAttribute("success", "Manager assigned to department");
        return "redirect:/company/admin";
    }
}
