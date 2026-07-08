package com.expensemanager.controller;

import com.expensemanager.model.User;
import com.expensemanager.service.NotificationService;
import com.expensemanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /** List all notifications and auto-mark them as read. */
    @GetMapping
    public String list(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("notifications", notificationService.getForUser(user));
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Notifications");
        model.addAttribute("activePage", "notifications");
        notificationService.markAllAsRead(user);
        return "notifications/list";
    }

    /** Mark a single notification as read. */
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, userService.getCurrentUser());
        return "redirect:/notifications";
    }

    /** Dismiss (delete) a single notification. */
    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id, RedirectAttributes ra) {
        notificationService.dismissOne(id, userService.getCurrentUser());
        ra.addFlashAttribute("success", "Notification dismissed.");
        return "redirect:/notifications";
    }

    /** Clear ALL notifications for the current user. */
    @PostMapping("/clear-all")
    public String clearAll(RedirectAttributes ra) {
        notificationService.clearAll(userService.getCurrentUser());
        ra.addFlashAttribute("success", "All notifications cleared.");
        return "redirect:/notifications";
    }
}
