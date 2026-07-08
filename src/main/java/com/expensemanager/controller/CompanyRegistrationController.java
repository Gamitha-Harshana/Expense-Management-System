package com.expensemanager.controller;

import com.expensemanager.dto.CompanyRegistrationDTO;
import com.expensemanager.model.Company;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/register/company")
@RequiredArgsConstructor
public class CompanyRegistrationController {

    private final CompanyService companyService;
    private final UserRepository userRepository;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("dto", new CompanyRegistrationDTO());
        return "company/register-company";
    }

    @PostMapping
    public String register(
            @Valid @ModelAttribute("dto") CompanyRegistrationDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttrs,
            Model model) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "duplicate", "This email is already registered");
        }

        if (dto.getConfirmPassword() != null &&
                !dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "company/register-company";
        }

        try {
            Company company = companyService.registerCompanyWithCeo(dto);
            redirectAttrs.addFlashAttribute("success",
                    "Welcome! Your company \"" + company.getName() + "\" has been registered. Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "company/register-company";
        }
    }
}
