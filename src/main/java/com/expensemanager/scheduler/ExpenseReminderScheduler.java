package com.expensemanager.scheduler;

import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.EmailService;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExpenseReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpenseReminderScheduler.class);

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final EmailService emailService;
    private final BudgetService budgetService;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public ExpenseReminderScheduler(UserRepository userRepository,
                                    ExpenseRepository expenseRepository,
                                    EmailService emailService,
                                    BudgetService budgetService) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.emailService = emailService;
        this.budgetService = budgetService;
    }

    /**
     * Runs every Monday at 9:00 AM — sends reminder to users with DRAFT expenses.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyDraftReminders() {
        if (!schedulerEnabled) return;
        log.info("Running weekly draft expense reminder...");

        List<User> users = userRepository.findAllActive();
        users.forEach(user -> {
            long draftCount = expenseRepository.countDraftsByUser(user);
            if (draftCount > 0) {
                log.info("Sending draft reminder to {} ({} drafts)", user.getEmail(), draftCount);
                emailService.sendDraftReminderEmail(user.getEmail(), user.getFullName(), draftCount);
            }
        });

        log.info("Weekly draft reminder completed.");
    }

    /**
     * Runs daily at 8:00 AM — checks for users near their budget limits.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkBudgetAlerts() {
        if (!schedulerEnabled) return;
        log.info("Running daily budget alert check...");
        budgetService.checkAndSendBudgetAlerts();
        log.info("Budget alert check completed.");
    }
}
