package com.expensemanager.service.impl;

import com.expensemanager.dto.BudgetDTO;
import com.expensemanager.model.BudgetLimit;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.NotificationType;
import com.expensemanager.repository.BudgetLimitRepository;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.EmailService;
import com.expensemanager.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private final BudgetLimitRepository budgetLimitRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public BudgetServiceImpl(BudgetLimitRepository budgetLimitRepository,
                             ExpenseRepository expenseRepository,
                             NotificationService notificationService,
                             EmailService emailService) {
        this.budgetLimitRepository = budgetLimitRepository;
        this.expenseRepository = expenseRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Override
    public BudgetLimit setLimit(BudgetDTO dto, User user) {
        int month = dto.getMonth() != null ? dto.getMonth() : LocalDate.now().getMonthValue();
        int year = dto.getYear() != null ? dto.getYear() : LocalDate.now().getYear();

        Optional<BudgetLimit> existing = budgetLimitRepository
                .findByUserAndCategoryAndMonthAndYear(user, dto.getCategory(), month, year);

        BudgetLimit limit = existing.orElseGet(BudgetLimit::new);
        limit.setUser(user);
        limit.setCategory(dto.getCategory());
        limit.setMonthlyLimit(dto.getMonthlyLimit());
        limit.setMonth(month);
        limit.setYear(year);
        limit.setAlertSent(false);

        // Recalculate current spend
        BigDecimal currentSpend = expenseRepository.sumByUserCategoryMonthYear(user, dto.getCategory(), month, year);
        limit.setCurrentSpend(currentSpend != null ? currentSpend : BigDecimal.ZERO);

        return budgetLimitRepository.save(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetLimit> getLimitsForCurrentMonth(User user) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return budgetLimitRepository.findByUserAndMonthAndYear(user, month, year);
    }

    @Override
    public void updateSpending(User user, ExpenseCategory category, BigDecimal amount, LocalDate expenseDate) {
        int month = expenseDate.getMonthValue();
        int year = expenseDate.getYear();

        budgetLimitRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year)
                .ifPresent(limit -> {
                    limit.setCurrentSpend(limit.getCurrentSpend().add(amount));
                    budgetLimitRepository.save(limit);

                    // Check if over 80%
                    if (!limit.isAlertSent() && limit.getUsagePercentage() >= 80) {
                        limit.setAlertSent(true);
                        budgetLimitRepository.save(limit);

                        notificationService.create(user,
                                "\u26a0\ufe0f Budget Warning: " + category.name(),
                                String.format("You've used %.1f%% of your %s budget",
                                        limit.getUsagePercentage(), category.name()),
                                NotificationType.WARNING,
                                "/budget");
                        emailService.sendBudgetWarningNotification(
                                user.getEmail(),
                                category.name(),
                                limit.getUsagePercentage());
                    }
                });
    }

    @Override
    public void deleteLimit(Long id, User user) {
        budgetLimitRepository.findById(id).ifPresent(limit -> {
            if (limit.getUser().getId().equals(user.getId())) {
                budgetLimitRepository.delete(limit);
            }
        });
    }

    @Override
    public void recalculateSpend(User user, ExpenseCategory category, LocalDate expenseDate) {
        int month = expenseDate.getMonthValue();
        int year  = expenseDate.getYear();
        budgetLimitRepository.findByUserAndCategoryAndMonthAndYear(user, category, month, year)
                .ifPresent(limit -> {
                    BigDecimal actual = expenseRepository.sumByUserCategoryMonthYear(user, category, month, year);
                    limit.setCurrentSpend(actual != null ? actual : BigDecimal.ZERO);
                    // Reset alert flag if spend dropped below threshold
                    if (limit.getUsagePercentage() < 80) {
                        limit.setAlertSent(false);
                    }
                    budgetLimitRepository.save(limit);
                });
    }

    @Override
    public void checkAndSendBudgetAlerts() {
        // Since this was a global scheduled task, it would need a custom query to find all users.
        // For now we'll comment out the global alert task implementation until we pass users context.
        /* List<BudgetLimit> limits = budgetLimitRepository.findBudgetsNeedingAlert();
        limits.forEach(limit -> {
            limit.setAlertSent(true);
            budgetLimitRepository.save(limit);
            notificationService.create(limit.getUser(),
                    "\u26a0\ufe0f Budget Warning",
                    String.format("%.1f%% of %s budget used",
                            limit.getUsagePercentage(), limit.getCategory().name()),
                    NotificationType.WARNING,
                    "/budget");
            emailService.sendBudgetWarningNotification(
                    limit.getUser().getEmail(),
                    limit.getCategory().name(),
                    limit.getUsagePercentage());
        }); */
    }
}
