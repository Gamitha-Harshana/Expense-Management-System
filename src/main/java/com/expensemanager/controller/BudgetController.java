package com.expensemanager.controller;

import com.expensemanager.dto.BudgetAllocationDTO;
import com.expensemanager.dto.BudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.BudgetLimitRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.EmployeeBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final EmployeeBudgetService employeeBudgetService;
    private final BudgetLimitRepository budgetLimitRepository;
    private final BudgetService         budgetService;
    private final UserRepository        userRepository;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Budget page — branches on role ────────────────────────────
    @GetMapping
    public String budgetPage(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = getCurrentUser(ud);

        // INDIVIDUAL: self-managed category budgets with date ranges
        if (user.getRole() == Role.ROLE_INDIVIDUAL) {
            List<BudgetLimit> limits = budgetService.getActiveLimitsForIndividual(user);
            model.addAttribute("limits",      limits);
            model.addAttribute("categories",  ExpenseCategory.values());
            model.addAttribute("budgetDTO",   new BudgetDTO());
            model.addAttribute("currentUser", user);
            model.addAttribute("pageTitle",   "My Budget");
            model.addAttribute("activePage",  "budget");
            return "budget/individual";
        }

        // EMPLOYEE: manager-assigned pool system
        List<EmployeeBudget> allPools    = employeeBudgetService.getAllForEmployee(user);
        List<EmployeeBudget> activePools = employeeBudgetService.getActiveForEmployee(user);

        Map<Long, List<BudgetLimit>> poolAllocations = new LinkedHashMap<>();
        Map<Long, BigDecimal>        poolAllocated   = new LinkedHashMap<>();
        Map<Long, BigDecimal>        poolRemaining   = new LinkedHashMap<>();

        for (EmployeeBudget pool : allPools) {
            poolAllocations.put(pool.getId(), budgetLimitRepository.findByEmployeeBudget(pool));
            poolAllocated  .put(pool.getId(), employeeBudgetService.getAllocatedAmount(pool));
            poolRemaining  .put(pool.getId(), employeeBudgetService.getRemainingAmount(pool));
        }

        model.addAttribute("pools",           allPools);
        model.addAttribute("activePools",     activePools);
        model.addAttribute("poolAllocations", poolAllocations);
        model.addAttribute("poolAllocated",   poolAllocated);
        model.addAttribute("poolRemaining",   poolRemaining);
        model.addAttribute("categories",      ExpenseCategory.values());
        model.addAttribute("allocationDTO",   new BudgetAllocationDTO());
        model.addAttribute("currentUser",     user);
        model.addAttribute("pageTitle",       "My Budget");
        model.addAttribute("activePage",      "budget");
        return "budget/index";
    }

    // ── INDIVIDUAL: set / update a category limit ─────────────────
    @PostMapping("/set")
    public String setLimit(@AuthenticationPrincipal UserDetails ud,
                           @Valid @ModelAttribute("budgetDTO") BudgetDTO dto,
                           BindingResult result,
                           RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please fill in all fields correctly.");
            return "redirect:/budget";
        }
        try {
            budgetService.setLimit(dto, user);
            ra.addFlashAttribute("success", "Budget limit saved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budget";
    }

    // ── INDIVIDUAL: delete a category limit ───────────────────────
    @PostMapping("/limit/{id}/delete")
    public String deleteLimit(@AuthenticationPrincipal UserDetails ud,
                              @PathVariable Long id,
                              RedirectAttributes ra) {
        budgetService.deleteLimit(id, getCurrentUser(ud));
        ra.addFlashAttribute("success", "Budget limit removed.");
        return "redirect:/budget";
    }

    // ── EMPLOYEE: allocate a portion of a pool into a category ────
    @PostMapping("/allocate")
    public String allocate(@AuthenticationPrincipal UserDetails ud,
                           @Valid @ModelAttribute("allocationDTO") BudgetAllocationDTO dto,
                           BindingResult result,
                           RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Please fill in all fields correctly.");
            return "redirect:/budget";
        }
        try {
            employeeBudgetService.allocate(dto, getCurrentUser(ud));
            ra.addFlashAttribute("success", "Budget allocated successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/budget";
    }

    // ── EMPLOYEE: delete an allocation ────────────────────────────
    @PostMapping("/allocation/{id}/delete")
    public String deleteAllocation(@AuthenticationPrincipal UserDetails ud,
                                   @PathVariable Long id,
                                   RedirectAttributes ra) {
        User employee = getCurrentUser(ud);
        budgetLimitRepository.findById(id).ifPresent(limit -> {
            if (limit.getUser().getId().equals(employee.getId())) {
                budgetLimitRepository.delete(limit);
            }
        });
        ra.addFlashAttribute("success", "Allocation removed.");
        return "redirect:/budget";
    }
}
