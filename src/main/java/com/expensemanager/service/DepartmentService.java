package com.expensemanager.service;

import com.expensemanager.dto.EmployeeInviteDTO;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;

import java.util.List;

public interface DepartmentService {
    /** Invite (create) a new employee under a manager's department. Returns user with plain password in departmentName field. */
    User inviteEmployee(User manager, Department department, EmployeeInviteDTO dto);
    /** Remove (soft-deactivate) an employee. */
    void removeEmployee(User employee);
    /** Get employees visible to a manager (their dept, or all company if no dept). */
    List<User> getEmployeesForManager(User manager);
    /** Get all departments managed by a manager. */
    List<Department> getDepartmentsForManager(User manager);
}
