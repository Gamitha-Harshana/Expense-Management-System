package com.expensemanager.service;

import com.expensemanager.dto.CompanyRegistrationDTO;
import com.expensemanager.dto.DepartmentDTO;
import com.expensemanager.dto.ManagerInviteDTO;
import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;

import java.util.List;
import java.util.Optional;

public interface CompanyService {
    Company registerCompanyWithCeo(CompanyRegistrationDTO dto);
    Optional<Company> findById(Long id);
    Optional<Company> findByCeo(User ceo);
    List<Company> findAll();
    Company save(Company company);

    // Department management
    Department createDepartment(Company company, DepartmentDTO dto);
    List<Department> getDepartmentsByCompany(Company company);
    Optional<Department> findDepartmentById(Long id);
    Department assignManagerToDepartment(Department dept, User manager);

    // Manager management
    User inviteManager(Company company, ManagerInviteDTO dto);
    void removeManager(User manager);
}
