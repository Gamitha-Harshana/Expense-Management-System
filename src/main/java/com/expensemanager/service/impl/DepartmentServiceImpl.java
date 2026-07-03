package com.expensemanager.service.impl;

import com.expensemanager.dto.EmployeeInviteDTO;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.DepartmentRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User inviteEmployee(User manager, Department department, EmployeeInviteDTO dto) {
        String tempPassword = UUID.randomUUID().toString().substring(0, 10);
        User employee = new User();
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setEmail(dto.getEmail());
        employee.setPassword(passwordEncoder.encode(tempPassword));
        employee.setRole(Role.ROLE_EMPLOYEE);
        employee.setCompany(manager.getCompany());
        employee.setDepartment(department);
        employee.setAddedBy(manager);
        employee.setEnabled(true);
        employee = userRepository.save(employee);
        // Pass plain password back via departmentName field (temporary carrier)
        employee.setDepartmentName(tempPassword);
        return employee;
    }

    @Override
    public void removeEmployee(User employee) {
        employee.setEnabled(false);
        userRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getEmployeesForManager(User manager) {
        List<Department> depts = departmentRepository.findByManager(manager);
        if (depts.isEmpty()) {
            // Manager has no department — see all company employees
            return userRepository.findEmployeesByCompany(manager.getCompany());
        }
        List<User> employees = new ArrayList<>();
        for (Department dept : depts) {
            employees.addAll(userRepository.findEmployeesByDepartment(dept));
        }
        return employees;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getDepartmentsForManager(User manager) {
        return departmentRepository.findByManager(manager);
    }
}
