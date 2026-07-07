package com.expensemanager.model.enums;

public enum Role {
    /** Platform-level administrator. Can see users and companies, never expenses. */
    ROLE_SITE_ADMIN,
    /** Company owner / CEO. Sees all expenses within their company. */
    ROLE_CEO,
    /** Department manager. Added by CEO. Can add/remove employees. */
    ROLE_MANAGER,
    /** Company employee. Added by manager only — cannot self-register. */
    ROLE_EMPLOYEE,
    /** Standalone personal user. No company, completely independent. */
    ROLE_INDIVIDUAL
}
