package com.expensemanager.controller;

import com.expensemanager.dto.BudgetAllocationDTO;
import com.expensemanager.dto.EmployeeBudgetDTO;
import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.DepartmentService;
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
@RequestMapping("/department/budget")
@RequiredArgsConstructor
public class ManagerBudgetController {

    private final EmployeeBudgetService employeeBudgetService;
    private final DepartmentService     departmentService;
    private final UserRepository        userRepository;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── Assign budget form ────────────────────────────────────────
    @GetMapping("/assign")
    public String assignForm(@AuthenticationPrincipal UserDetails ud,
                             @RequestParam(required = false) Long employeeId,
                             Model model) {
        User manager = getCurrentUser(ud);
        List<User> employees = departmentService.getEmployeesForManager(manager);

        model.addAttribute("employees", employees);
        model.addAttribute("preselectedEmployeeId", employeeId);
        model.addAttribute("dto", new EmployeeBudgetDTO());
        model.addAttribute("pageTitle", "Assign Budget");
        model.addAttribute("activePage", "department");
        return "budget/assign";
    }

    // ── Save new pool ─────────────────────────────────────────────
    @PostMapping("/assign")
    public String assignSave(@AuthenticationPrincipal UserDetails ud,
                             @Valid @ModelAttribute("dto") EmployeeBudgetDTO dto,
                             BindingResult result,
                             RedirectAttributes ra,
                             Model model) {
        User manager = getCurrentUser(ud);
        if (result.hasErrors()) {
            model.addAttribute("employees", departmentService.getEmployeesForManager(manager));
            model.addAttribute("pageTitle", "Assign Budget");
            model.addAttribute("activePage", "department");
            return "budget/assign";
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            ra.addFlashAttribute("error", "End date must be after start date.");
            return "redirect:/department/budget/assign";
        }
        EmployeeBudget pool = employeeBudgetService.assign(dto, manager);
        String name = pool.getEmployee().getFullName();
        ra.addFlashAttribute("success",
                "Budget of " + pool.getTotalAmount() + " assigned to " + name + ".");
        return "redirect:/department/manage";
    }

    // ── View all pools this manager has assigned ──────────────────
    @GetMapping
    public String listPools(@AuthenticationPrincipal UserDetails ud, Model model) {
        User manager = getCurrentUser(ud);
        List<EmployeeBudget> pools = employeeBudgetService.getAllAssignedByManager(manager);

        Map<Long, BigDecimal> remaining = new LinkedHashMap<>();
        Map<Long, BigDecimal> allocated = new LinkedHashMap<>();
        pools.forEach(p -> {
            remaining.put(p.getId(), employeeBudgetService.getRemainingAmount(p));
            allocated.put(p.getId(), employeeBudgetService.getAllocatedAmount(p));
        });

        model.addAttribute("pools",     pools);
        model.addAttribute("remaining", remaining);
        model.addAttribute("allocated", allocated);
        model.addAttribute("pageTitle", "Team Budgets");
        model.addAttribute("activePage", "department");
        return "budget/manager-pools";
    }

    // ── Delete a pool ─────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deletePool(@AuthenticationPrincipal UserDetails ud,
                             @PathVariable Long id,
                             RedirectAttributes ra) {
        employeeBudgetService.delete(id, getCurrentUser(ud));
        ra.addFlashAttribute("success", "Budget pool removed.");
        return "redirect:/department/budget";
    }
}
