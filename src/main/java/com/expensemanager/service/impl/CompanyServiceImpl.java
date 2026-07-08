package com.expensemanager.service.impl;

import com.expensemanager.dto.CompanyRegistrationDTO;
import com.expensemanager.dto.DepartmentDTO;
import com.expensemanager.dto.ManagerInviteDTO;
import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.CompanyRepository;
import com.expensemanager.repository.DepartmentRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Company registerCompanyWithCeo(CompanyRegistrationDTO dto) {
        // 1. Create CEO user
        User ceo = new User();
        ceo.setFirstName(dto.getFirstName());
        ceo.setLastName(dto.getLastName());
        ceo.setEmail(dto.getEmail());
        ceo.setPassword(passwordEncoder.encode(dto.getPassword()));
        ceo.setRole(Role.ROLE_CEO);
        ceo.setEnabled(true);

        // 2. Create Company
        Company company = new Company();
        company.setName(dto.getCompanyName());
        company.setIndustry(dto.getIndustry());
        company.setCountry(dto.getCountry());
        company.setSize(dto.getSize());
        company.setRegistrationNumber(dto.getRegistrationNumber());
        company.setAddress(dto.getAddress());
        company.setPhone(dto.getPhone());
        company.setWebsite(dto.getWebsite());
        company.setActive(true);


        // 3. Link CEO to company
        company = companyRepository.save(company);
        ceo.setCompany(company);
        ceo = userRepository.save(ceo);
        company.setCeo(ceo);
        company = companyRepository.save(company);

        return company;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findById(Long id) {
        return companyRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findByCeo(User ceo) {
        return companyRepository.findByCeo(ceo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAll() {
        return companyRepository.findAllByActiveTrue();
    }

    @Override
    public Company save(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public Department saveDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department createDepartment(Company company, DepartmentDTO dto) {
        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setCompany(company);
        if (dto.getManagerId() != null) {
            userRepository.findById(dto.getManagerId())
                    .ifPresent(dept::setManager);
        }
        return departmentRepository.save(dept);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getDepartmentsByCompany(Company company) {
        return departmentRepository.findByCompany(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    @Override
    public Department assignManagerToDepartment(Department dept, User manager) {
        dept.setManager(manager);
        return departmentRepository.save(dept);
    }

    @Override
    public User inviteManager(Company company, ManagerInviteDTO dto) {
        String tempPassword = UUID.randomUUID().toString().substring(0, 10);
        User manager = new User();
        manager.setFirstName(dto.getFirstName());
        manager.setLastName(dto.getLastName());
        manager.setEmail(dto.getEmail());
        manager.setPassword(passwordEncoder.encode(tempPassword));
        manager.setRole(Role.ROLE_MANAGER);
        manager.setCompany(company);
        manager.setEnabled(true);
        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId())
                    .ifPresent(manager::setDepartment);
        }
        manager = userRepository.save(manager);
        // Store plain password temporarily so controller can display it once
        manager.setDepartmentName(tempPassword); // borrowing field to pass temp password
        return manager;
    }

    @Override
    public void removeManager(User manager) {
        manager.setEnabled(false);
        userRepository.save(manager);
    }
}
