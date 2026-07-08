package com.expensemanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ManagerInviteDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    /** Optional — manager can be assigned to one or more departments later. */
    private Long departmentId;
}
