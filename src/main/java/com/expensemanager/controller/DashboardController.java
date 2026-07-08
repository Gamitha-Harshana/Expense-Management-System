package com.expensemanager.controller;

import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.NotificationService;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;
    private final BudgetService budgetService;
    private final CompanyService companyService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = userRepository.findByEmail(ud.getUsername()).orElseThrow();
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");

        long unreadCount = notificationService.countUnread(user);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("recentNotifications",
                notificationService.getForUser(user).stream().limit(5).toList());

        Role role = user.getRole();

        if (role == Role.ROLE_SITE_ADMIN) {
            model.addAttribute("companyCount", companyService.findAll().size());
            model.addAttribute("userCount", userRepository.findAllActive().size());
            return "dashboard/index";
        }

        if (role == Role.ROLE_CEO) {
            return "redirect:/company/admin";
        }

        if (role == Role.ROLE_MANAGER) {
            var company = user.getCompany();
            if (company != null) {
                List<Department> managerDepts = companyService.getDepartmentsByCompany(company)
                        .stream()
                        .filter(d -> d.getManager() != null && d.getManager().getId().equals(user.getId()))
                        .toList();
                if (!managerDepts.isEmpty()) {
                    long pending = managerDepts.stream()
                            .mapToLong(d -> expenseRepository.countPendingByDepartment(d))
                            .sum();
                    model.addAttribute("pendingApprovals", pending);
                    model.addAttribute("pendingExpenses",
                            expenseRepository.findPendingByDepartment(managerDepts.get(0)));
                } else {
                    model.addAttribute("pendingApprovals", expenseRepository.countPendingByCompany(company));
                    model.addAttribute("pendingExpenses", expenseRepository.findPendingByCompany(company));
                }
            }
            return "dashboard/index";
        }

        // EMPLOYEE or INDIVIDUAL
        model.addAttribute("draftCount", expenseRepository.countDraftsByUser(user));
        model.addAttribute("submittedCount", expenseRepository.countSubmittedByUser(user));
        model.addAttribute("budgetLimits", budgetService.getLimitsForCurrentMonth(user));
        return "dashboard/index";
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        // Logged-in users go straight to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "landing";
    }
}
