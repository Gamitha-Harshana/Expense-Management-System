package com.expensemanager.controller;

import com.expensemanager.model.User;
import com.expensemanager.service.NotificationService;
import com.expensemanager.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String listNotifications(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("notifications", notificationService.getForUser(currentUser));
        model.addAttribute("currentUser", currentUser);
        notificationService.markAllAsRead(currentUser);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        notificationService.markAsRead(id, currentUser);
        return "redirect:/notifications";
    }
}
