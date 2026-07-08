package com.expensemanager.repository;

import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByCompany(Company company);
    List<Department> findByManager(User manager);
    boolean existsByNameAndCompany(String name, Company company);
}
