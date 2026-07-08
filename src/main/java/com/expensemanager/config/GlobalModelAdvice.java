package com.expensemanager.config;

import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Automatically injects the full User entity and unread notification
     * count into every controller's model, so base.html can always display
     * the correct name, avatar initial, role, and badge count.
     */
    @ModelAttribute
    public void addGlobalAttributes(
            @AuthenticationPrincipal UserDetails userDetails,
            org.springframework.ui.Model model) {

        if (userDetails == null) return; // not logged in (auth pages)

        userRepository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            model.addAttribute("currentUser", user);
            model.addAttribute("unreadCount",
                    notificationService.countUnread(user));
        });
    }
}
