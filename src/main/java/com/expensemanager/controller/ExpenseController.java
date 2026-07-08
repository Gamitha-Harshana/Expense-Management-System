package com.expensemanager.controller;

import com.expensemanager.dto.ExpenseDTO;
import com.expensemanager.dto.ExpenseSearchDTO;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.model.enums.ExpenseType;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import com.expensemanager.service.EmployeeBudgetService;
import com.expensemanager.service.ExpenseService;
import com.expensemanager.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CompanyService companyService;
    private final EmployeeBudgetService employeeBudgetService;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername()).orElseThrow();
    }

    // ── List ───────────────────────────────────────────────────────
    @GetMapping
    public String list(
            @AuthenticationPrincipal UserDetails ud,
            ExpenseSearchDTO searchDTO,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        User user = getCurrentUser(ud);
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Expenses");
        model.addAttribute("activePage", "expenses");
        model.addAttribute("unreadCount",
                userRepository.findByEmail(ud.getUsername()).map(u ->
                        (long) notificationService.countUnread(u)).orElse(0L));

        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Expense> expenses;
        Role role = user.getRole();

        if (role == Role.ROLE_CEO) {
            // CEO sees all company expenses
            var company = companyService.findByCeo(user).orElse(null);
            expenses = (company != null)
                    ? expenseRepository.searchExpenses(null, company, null,
                        searchDTO.getKeyword(), searchDTO.getCategory(),
                        searchDTO.getStatus(), searchDTO.getExpenseType(),
                        searchDTO.getMinAmount(), searchDTO.getMaxAmount(),
                        searchDTO.getFromDate(), searchDTO.getToDate(), pageable)
                    : Page.empty();
        } else if (role == Role.ROLE_MANAGER) {
            // Manager sees their department(s), or all company if no dept
            var depts = companyService.getDepartmentsByCompany(user.getCompany())
                    .stream().filter(d -> d.getManager() != null &&
                            d.getManager().getId().equals(user.getId())).toList();
            if (!depts.isEmpty()) {
                expenses = expenseRepository.searchExpenses(null, null, depts.get(0),
                        searchDTO.getKeyword(), searchDTO.getCategory(),
                        searchDTO.getStatus(), searchDTO.getExpenseType(),
                        searchDTO.getMinAmount(), searchDTO.getMaxAmount(),
                        searchDTO.getFromDate(), searchDTO.getToDate(), pageable);
            } else {
                expenses = expenseRepository.searchExpenses(null, user.getCompany(), null,
                        searchDTO.getKeyword(), searchDTO.getCategory(),
                        searchDTO.getStatus(), searchDTO.getExpenseType(),
                        searchDTO.getMinAmount(), searchDTO.getMaxAmount(),
                        searchDTO.getFromDate(), searchDTO.getToDate(), pageable);
            }
        } else {
            // EMPLOYEE or INDIVIDUAL — own expenses only
            expenses = expenseRepository.searchExpenses(user, null, null,
                    searchDTO.getKeyword(), searchDTO.getCategory(),
                    searchDTO.getStatus(), searchDTO.getExpenseType(),
                    searchDTO.getMinAmount(), searchDTO.getMaxAmount(),
                    searchDTO.getFromDate(), searchDTO.getToDate(), pageable);
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("searchDTO", searchDTO);
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("types", ExpenseType.values());
        return "expenses/list";
    }

    // ── New Form ──────────────────────────────────────────────────────
    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails ud,
                          Model model,
                          RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        // Site admin cannot create expenses
        if (user.getRole() == Role.ROLE_SITE_ADMIN) return "redirect:/dashboard";
        // Employees must have at least one active budget pool assigned by their manager
        if (user.getRole() == Role.ROLE_EMPLOYEE) {
            boolean hasActiveBudget = !employeeBudgetService.getActiveForEmployee(user).isEmpty();
            if (!hasActiveBudget) {
                ra.addFlashAttribute("error",
                    "You don't have an active budget allocated yet. " +
                    "Please ask your manager to assign a budget before adding expenses.");
                return "redirect:/expenses";
            }
        }
        model.addAttribute("currentUser", user);
        model.addAttribute("expenseDTO", new ExpenseDTO());
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("types", ExpenseType.values());
        model.addAttribute("pageTitle", "New Expense");
        model.addAttribute("activePage", "expenses");
        return "expenses/form";
    }

    // ── Create ───────────────────────────────────────────────────────
    @PostMapping
    public String create(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @ModelAttribute("expenseDTO") ExpenseDTO expense,
            BindingResult result,
            @RequestParam(value = "receiptFile", required = false) MultipartFile receiptFile,
            RedirectAttributes ra,
            Model model) {

        User user = getCurrentUser(ud);
        // Double-check: employee must still have an active budget at submit time
        if (user.getRole() == Role.ROLE_EMPLOYEE) {
            boolean hasActiveBudget = !employeeBudgetService.getActiveForEmployee(user).isEmpty();
            if (!hasActiveBudget) {
                ra.addFlashAttribute("error",
                    "Cannot save expense — you have no active budget. " +
                    "Contact your manager to allocate one.");
                return "redirect:/expenses";
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("categories", ExpenseCategory.values());
            model.addAttribute("types", ExpenseType.values());
            return "expenses/form";
        }
        expense.setReceiptFile(receiptFile);
        expenseService.create(expense, user);
        ra.addFlashAttribute("success", "Expense created successfully");
        return "redirect:/expenses";
    }

    // ── Detail View ──────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detail(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            Model model) {
        User user = getCurrentUser(ud);
        Expense Expense = expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        // Access guard
        if (!canAccess(user, Expense)) return "redirect:/expenses";
        model.addAttribute("currentUser", user);
        model.addAttribute("expense", Expense);
        model.addAttribute("pageTitle", "Expense Detail");
        model.addAttribute("activePage", "expenses");
        return "expenses/detail";
    }

    // ── Edit Form ──────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String editForm(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id, Model model) {
        User user = getCurrentUser(ud);
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(id).orElseThrow();
        if (!expense.getUser().getId().equals(user.getId()) || !expense.isEditable())
            return "redirect:/expenses";
        model.addAttribute("currentUser", user);
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setTitle(expense.getTitle());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setCategory(expense.getCategory());
        dto.setExpenseType(expense.getExpenseType());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setNotes(expense.getNotes());
        
        model.addAttribute("expenseDTO", dto);
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("types", ExpenseType.values());
        model.addAttribute("pageTitle", "Edit Expense");
        model.addAttribute("activePage", "expenses");
        return "expenses/form";
    }

    // ── Update ───────────────────────────────────────────────────────
    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            @Valid @ModelAttribute("expenseDTO") ExpenseDTO expense,
            BindingResult result,
            @RequestParam(value = "receiptFile", required = false) MultipartFile receiptFile,
            @RequestParam(value = "_method", required = false) String method,
            RedirectAttributes ra,
            Model model) {

        User user = getCurrentUser(ud);
        if ("DELETE".equalsIgnoreCase(method)) {
            expenseService.softDelete(id, user);
            ra.addFlashAttribute("success", "Expense deleted");
            return "redirect:/expenses";
        }
        if (result.hasErrors()) {
            model.addAttribute("currentUser", user);
            model.addAttribute("categories", ExpenseCategory.values());
            model.addAttribute("types", ExpenseType.values());
            return "expenses/form";
        }
        expense.setReceiptFile(receiptFile);
        expenseService.update(id, expense, user);
        ra.addFlashAttribute("success", "Expense updated");
        return "redirect:/expenses/" + id;
    }

    // ── Submit for Approval ──────────────────────────────────────────
    @PostMapping("/{id}/submit")
    public String submit(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        expenseService.submit(id, user);
        ra.addFlashAttribute("success", "Expense submitted for approval");
        return "redirect:/expenses/" + id;
    }

    // ── Approve ──────────────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    public String approve(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        if (user.getRole() != Role.ROLE_CEO && user.getRole() != Role.ROLE_MANAGER)
            return "redirect:/expenses";
        expenseService.approve(id, user);
        ra.addFlashAttribute("success", "Expense approved");
        return "redirect:/expenses/" + id;
    }

    // ── Reject ───────────────────────────────────────────────────────
    @PostMapping("/{id}/reject")
    public String reject(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes ra) {
        User user = getCurrentUser(ud);
        if (user.getRole() != Role.ROLE_CEO && user.getRole() != Role.ROLE_MANAGER)
            return "redirect:/expenses";
        expenseService.reject(id, user, reason);
        ra.addFlashAttribute("success", "Expense rejected");
        return "redirect:/expenses/" + id;
    }

    // ── Access Guard ─────────────────────────────────────────────────
    private boolean canAccess(User user, Expense expense) {
        return switch (user.getRole()) {
            case ROLE_SITE_ADMIN -> false; // site admin never sees expenses
            case ROLE_CEO -> expense.getUser().getCompany() != null &&
                    user.getCompany() != null &&
                    expense.getUser().getCompany().getId().equals(user.getCompany().getId());
            case ROLE_MANAGER -> expense.getUser().getCompany() != null &&
                    user.getCompany() != null &&
                    expense.getUser().getCompany().getId().equals(user.getCompany().getId());
            case ROLE_EMPLOYEE, ROLE_INDIVIDUAL -> expense.getUser().getId().equals(user.getId());
        };
    }
}
