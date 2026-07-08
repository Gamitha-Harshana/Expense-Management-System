package com.expensemanager.controller;

import com.expensemanager.dto.BudgetDTO;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/budget")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    public BudgetController(BudgetService budgetService, UserService userService) {
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @GetMapping
    public String manageBudget(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("budgetLimits", budgetService.getLimitsForCurrentMonth(currentUser));
        model.addAttribute("budgetDTO", new BudgetDTO());
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("currentUser", currentUser);
        return "budget/manage";
    }

    @PostMapping
    public String setBudget(@Valid @ModelAttribute("budgetDTO") BudgetDTO dto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("budgetLimits", budgetService.getLimitsForCurrentMonth(currentUser));
            model.addAttribute("categories", ExpenseCategory.values());
            model.addAttribute("currentUser", currentUser);
            return "budget/manage";
        }
        User currentUser = userService.getCurrentUser();
        budgetService.setLimit(dto, currentUser);
        redirectAttributes.addFlashAttribute("success", "Budget limit saved!");
        return "redirect:/budget";
    }

    @PostMapping("/{id}/delete")
    public String deleteBudget(@PathVariable Long id, RedirectAttributes ra) {
        budgetService.deleteLimit(id, userService.getCurrentUser());
        ra.addFlashAttribute("success", "Budget limit deleted.");
        return "redirect:/budget";
    }
}
