package com.expensemanager.model;

import com.expensemanager.model.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** The company this user belongs to. Null for INDIVIDUAL and SITE_ADMIN. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /** The department this user belongs to. Applicable to EMPLOYEE only. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /** Who created / invited this user. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private User addedBy;

    /** Generic department string (kept for backward compat / display). */
    private String departmentName;

    private boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String profilePicture;

    /** Soft-delete timestamp. */
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ── Helpers ─────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isSiteAdmin() {
        return role == Role.ROLE_SITE_ADMIN;
    }

    public boolean isCeo() {
        return role == Role.ROLE_CEO;
    }

    public boolean isManager() {
        return role == Role.ROLE_MANAGER;
    }

    public boolean isEmployee() {
        return role == Role.ROLE_EMPLOYEE;
    }

    public boolean isIndividual() {
        return role == Role.ROLE_INDIVIDUAL;
    }

    /** Returns true if user belongs to a company (CEO, MANAGER, EMPLOYEE). */
    public boolean hasCompany() {
        return company != null;
    }
}
