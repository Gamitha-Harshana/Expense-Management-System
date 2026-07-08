package com.expensemanager.dto;

import com.expensemanager.model.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @Email(message = "Valid email required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    private Role role = Role.ROLE_INDIVIDUAL;
}
