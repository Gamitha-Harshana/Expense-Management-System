package com.expensemanager;

import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class ExpenseManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseManagementApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                // Admin
                User admin = new User();
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setEmail("admin@expense.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ROLE_SITE_ADMIN);
                admin.setDepartmentName("IT");
                admin.setEnabled(true);
                userRepository.save(admin);

                // Manager
                User manager = new User();
                manager.setFirstName("Jane");
                manager.setLastName("Smith");
                manager.setEmail("manager@expense.com");
                manager.setPassword(passwordEncoder.encode("manager123"));
                manager.setRole(Role.ROLE_MANAGER);
                manager.setDepartmentName("Finance");
                manager.setEnabled(true);
                userRepository.save(manager);

                // Corporate Employee
                User employee = new User();
                employee.setFirstName("John");
                employee.setLastName("Doe");
                employee.setEmail("employee@expense.com");
                employee.setPassword(passwordEncoder.encode("employee123"));
                employee.setRole(Role.ROLE_EMPLOYEE);
                employee.setDepartmentName("Sales");
                employee.setEnabled(true);
                userRepository.save(employee);

                // Individual User
                User individual = new User();
                individual.setFirstName("Alice");
                individual.setLastName("Brown");
                individual.setEmail("individual@expense.com");
                individual.setPassword(passwordEncoder.encode("individual123"));
                individual.setRole(Role.ROLE_INDIVIDUAL);
                individual.setDepartmentName("N/A");
                individual.setEnabled(true);
                userRepository.save(individual);

                System.out.println("========================================");
                System.out.println("  Seed data loaded successfully!");
                System.out.println("  Admin:      admin@expense.com / admin123");
                System.out.println("  Manager:    manager@expense.com / manager123");
                System.out.println("  Employee:   employee@expense.com / employee123");
                System.out.println("  Individual: individual@expense.com / individual123");
                System.out.println("========================================");
            }
        };
    }
}
