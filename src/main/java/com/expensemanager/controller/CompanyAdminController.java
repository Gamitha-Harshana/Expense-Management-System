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
import java.util.List;
import java.util.Optional;
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

    private Optional<Company> getCompanyOpt(User user) {
        return companyService.findByCeo(user);
    }

    // ── Company Overview ─────────────────────────────────────────
    @GetMapping("/admin")
    public String overview(@AuthenticationPrincipal UserDetails ud, Model model,
                           RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error",
                "No company found for your account. Please register a company first.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
        List<com.expensemanager.model.Department> departments =
                companyService.getDepartmentsByCompany(company);
        List<User> managers = userRepository.findManagersByCompany(company);
        List<User> employees = userRepository.findEmployeesByCompany(company);

        // Build a map of managerId → employee count for the template
        java.util.Map<Long, Long> mgrEmployeeCounts = new java.util.HashMap<>();
        for (User mgr : managers) {
            if (mgr.getDepartment() != null) {
                long count = employees.stream()
                        .filter(e -> mgr.getDepartment().equals(e.getDepartment()))
                        .count();
                mgrEmployeeCounts.put(mgr.getId(), count);
            } else {
                mgrEmployeeCounts.put(mgr.getId(), 0L);
            }
        }

        model.addAttribute("company", company);
        model.addAttribute("departments", departments);
        model.addAttribute("managers", managers);
        model.addAttribute("employees", employees);
        model.addAttribute("employeeCount", employees.size());
        model.addAttribute("mgrEmployeeCounts", mgrEmployeeCounts);
        model.addAttribute("pendingCount", 0); // placeholder — expand later
        model.addAttribute("departmentDTO", new DepartmentDTO());
        model.addAttribute("managerDTO", new ManagerInviteDTO());
        model.addAttribute("pageTitle", "Company Admin");
        model.addAttribute("activePage", "company");
        return "company/admin";
    }

    // ── Edit Department Form ──────────────────────────────────────
    @GetMapping("/admin/departments/{id}/edit")
    public String editDepartmentForm(@AuthenticationPrincipal UserDetails ud,
                                     @PathVariable Long id,
                                     Model model,
                                     RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
        return companyService.findDepartmentById(id)
                .filter(dept -> dept.getCompany().getId().equals(company.getId()))
                .map(dept -> {
                    DepartmentDTO dto = new DepartmentDTO();
                    dto.setName(dept.getName());
                    if (dept.getManager() != null) dto.setManagerId(dept.getManager().getId());
                    model.addAttribute("departmentDTO", dto);
                    model.addAttribute("dept", dept);
                    model.addAttribute("managers", userRepository.findManagersByCompany(company));
                    model.addAttribute("pageTitle", "Edit Department");
                    model.addAttribute("activePage", "company");
                    return "company/edit-department";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Department not found.");
                    return "redirect:/company/admin";
                });
    }

    // ── Save Edited Department ────────────────────────────────────
    @PostMapping("/admin/departments/{id}/edit")
    public String editDepartmentSave(@AuthenticationPrincipal UserDetails ud,
                                     @PathVariable Long id,
                                     @Valid @ModelAttribute("departmentDTO") DepartmentDTO dto,
                                     BindingResult result,
                                     RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Department name is required.");
            return "redirect:/company/admin/departments/" + id + "/edit";
        }
        Company company = companyOpt.get();
        companyService.findDepartmentById(id).ifPresent(dept -> {
            if (dept.getCompany().getId().equals(company.getId())) {
                dept.setName(dto.getName());
                if (dto.getManagerId() != null) {
                    userRepository.findById(dto.getManagerId()).ifPresent(dept::setManager);
                } else {
                    dept.setManager(null);
                }
                companyService.saveDepartment(dept);
            }
        });
        ra.addFlashAttribute("success", "Department updated successfully.");
        return "redirect:/company/admin";
    }

    // ── Add Manager Form ──────────────────────────────────────────
    @GetMapping("/admin/add-manager")
    public String addManagerForm(@AuthenticationPrincipal UserDetails ud, Model model,
                                 RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
        model.addAttribute("departments", companyService.getDepartmentsByCompany(company));
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Add Manager");
        model.addAttribute("activePage", "company");
        return "company/add-manager";
    }

    // ── Add Department Form ───────────────────────────────────────
    @GetMapping("/admin/add-department")
    public String addDepartmentForm(@AuthenticationPrincipal UserDetails ud, Model model,
                                    RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
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
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Department name is required");
            return "redirect:/company/admin";
        }
        companyService.createDepartment(companyOpt.get(), dto);
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
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
        companyService.findDepartmentById(id).ifPresent(dept -> {
            if (dept.getCompany().getId().equals(company.getId())) {
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
        Optional<Company> companyOpt = getCompanyOpt(user);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please fill in all required fields");
            return "redirect:/company/admin";
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            ra.addFlashAttribute("error", "Email already registered: " + dto.getEmail());
            return "redirect:/company/admin";
        }
        User manager = companyService.inviteManager(companyOpt.get(), dto);
        String tempPwd = manager.getDepartmentName();
        manager.setDepartmentName(null);
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
        Optional<Company> companyOpt = getCompanyOpt(ceo);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
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
        Optional<Company> companyOpt = getCompanyOpt(ceo);
        if (companyOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No company found for your account.");
            return "redirect:/dashboard";
        }
        Company company = companyOpt.get();
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
