package com.expensemanager.controller;

import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CompanyService companyService;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Users ───────────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails ud, Model model) {
        User admin = getCurrentUser(ud);
        model.addAttribute("currentUser", admin);
        model.addAttribute("users", userRepository.findAllActive());
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("activePage", "admin");
        // No expense data exposed to site admin
        return "admin/users";
    }

    // ── Companies ──────────────────────────────────────────────────
    @GetMapping("/companies")
    public String companies(@AuthenticationPrincipal UserDetails ud, Model model) {
        User admin = getCurrentUser(ud);
        model.addAttribute("currentUser", admin);
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("pageTitle", "Companies");
        model.addAttribute("activePage", "companies");
        return "admin/companies";
    }

    // ── Deactivate User ─────────────────────────────────────────────
    @PostMapping("/users/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {
        User admin = getCurrentUser(ud);
        userRepository.findById(id).ifPresent(u -> {
            if (!u.getId().equals(admin.getId()) && u.getRole() != Role.ROLE_SITE_ADMIN) {
                u.setEnabled(false);
                userRepository.save(u);
            }
        });
        ra.addFlashAttribute("success", "User deactivated");
        return "redirect:/admin/users";
    }

    // ── Activate User ──────────────────────────────────────────────
    @PostMapping("/users/{id}/activate")
    public String activate(
            @PathVariable Long id,
            RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setEnabled(true);
            userRepository.save(u);
        });
        ra.addFlashAttribute("success", "User activated");
        return "redirect:/admin/users";
    }

    @GetMapping("/audit")
    public String audit(@AuthenticationPrincipal UserDetails ud, Model model) {
        User admin = getCurrentUser(ud);
        model.addAttribute("currentUser", admin);
        model.addAttribute("pageTitle", "Audit Trail");
        model.addAttribute("activePage", "audit");
        return "admin/audit";
    }
}
