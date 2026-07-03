package com.expensemanager.controller;

import com.expensemanager.dto.UserRegistrationDTO;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage(
            @AuthenticationPrincipal UserDetails ud,
            Model model) {
        if (ud != null) return "redirect:/dashboard";
        return "auth/login";
    }

    /** Public registration page — choice between Company (CEO) or Individual. */
    @GetMapping("/register")
    public String registerPage(
            @AuthenticationPrincipal UserDetails ud,
            Model model) {
        if (ud != null) return "redirect:/dashboard";
        model.addAttribute("dto", new UserRegistrationDTO());
        return "auth/register";
    }

    /** Handles INDIVIDUAL self-registration only. CEO uses /register/company. */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("dto") UserRegistrationDTO dto,
            BindingResult result,
            RedirectAttributes ra,
            Model model) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "duplicate", "This email is already registered");
        }
        if (result.hasErrors()) {
            model.addAttribute("showIndForm", true);
            return "auth/register";
        }
        // Force individual role — employees cannot self-register
        dto.setRole(Role.ROLE_INDIVIDUAL);
        userService.register(dto);
        ra.addFlashAttribute("success", "Account created! Please log in.");
        return "redirect:/login";
    }
}
