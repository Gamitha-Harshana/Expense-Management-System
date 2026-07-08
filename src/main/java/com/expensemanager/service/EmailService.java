package com.expensemanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@expensemanager.com}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendExpenseSubmittedNotification(String managerEmail, String employeeName, String expenseTitle, Long expenseId) {
        String subject = "[EMS] New Expense Requires Your Approval";
        String body = String.format("""
                Hello,

                %s has submitted a new expense for your approval.

                Expense: %s
                View: http://localhost:8080/expenses/%d

                Please log in to review and approve or reject this expense.

                — Expense Management System
                """, employeeName, expenseTitle, expenseId);
        send(managerEmail, subject, body);
    }

    public void sendExpenseApprovedNotification(String employeeEmail, String expenseTitle) {
        String subject = "[EMS] ✅ Your Expense Has Been Approved";
        String body = String.format("""
                Hello,

                Great news! Your expense '%s' has been approved.

                It will be processed for reimbursement according to your company's schedule.

                — Expense Management System
                """, expenseTitle);
        send(employeeEmail, subject, body);
    }

    public void sendExpenseRejectedNotification(String employeeEmail, String expenseTitle, String reason) {
        String subject = "[EMS] ❌ Your Expense Was Rejected";
        String body = String.format("""
                Hello,

                Your expense '%s' has been rejected.

                Reason: %s

                Please update your expense and resubmit if appropriate.

                — Expense Management System
                """, expenseTitle, reason);
        send(employeeEmail, subject, body);
    }

    public void sendBudgetWarningNotification(String userEmail, String category, double percentage) {
        String subject = "[EMS] ⚠️ Budget Warning: " + category;
        String body = String.format("""
                Hello,

                You have used %.1f%% of your monthly budget for %s.

                Consider reviewing your spending to stay within budget.

                — Expense Management System
                """, percentage, category);
        send(userEmail, subject, body);
    }

    public void sendDraftReminderEmail(String userEmail, String userName, long draftCount) {
        String subject = "[EMS] Reminder: You have " + draftCount + " pending expense(s) to submit";
        String body = String.format("""
                Hello %s,

                This is a friendly reminder that you have %d draft expense(s) waiting to be submitted.

                Please log in to submit your expenses for approval.

                — Expense Management System
                """, userName, draftCount);
        send(userEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[EMAIL MOCK] To: {} | Subject: {} | Body: {}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
