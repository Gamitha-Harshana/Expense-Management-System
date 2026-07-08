package com.expensemanager.controller;

import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Show settings page ────────────────────────────────────────
    @GetMapping
    public String showProfile(@AuthenticationPrincipal UserDetails ud, Model model) {
        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("activePage", "profile");
        return "profile/settings";
    }

    // ── Change password (all users) ───────────────────────────────
    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        User user = getCurrentUser(ud);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("pwdError", "Current password is incorrect.");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("pwdError", "New password must be at least 6 characters.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwdError", "New passwords do not match.");
            return "redirect:/profile";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("pwdSuccess", "Password changed successfully.");
        return "redirect:/profile";
    }

    // ── Change email (INDIVIDUAL only) ────────────────────────────
    @PostMapping("/change-email")
    public String changeEmail(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam String newEmail,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        User user = getCurrentUser(ud);

        if (!user.isIndividual()) {
            ra.addFlashAttribute("emailError", "Only individual accounts can change their email.");
            return "redirect:/profile";
        }
        if (newEmail == null || !newEmail.contains("@")) {
            ra.addFlashAttribute("emailError", "Please enter a valid email address.");
            return "redirect:/profile";
        }
        if (!passwordEncoder.matches(confirmPassword, user.getPassword())) {
            ra.addFlashAttribute("emailError", "Password confirmation is incorrect.");
            return "redirect:/profile";
        }
        if (userRepository.existsByEmail(newEmail) && !newEmail.equalsIgnoreCase(user.getEmail())) {
            ra.addFlashAttribute("emailError", "That email is already registered.");
            return "redirect:/profile";
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        ra.addFlashAttribute("emailSuccess",
                "Email updated to " + newEmail + ". Please log in again with your new email.");
        return "redirect:/profile";
    }
}
