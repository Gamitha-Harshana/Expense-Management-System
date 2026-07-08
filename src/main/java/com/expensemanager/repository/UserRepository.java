package com.expensemanager.repository;

import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Active users only (not soft-deleted). */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.role = :role")
    List<User> findActiveByRole(@Param("role") Role role);

    /** All active users in a company. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.company = :company ORDER BY u.firstName")
    List<User> findActiveByCompany(@Param("company") Company company);

    /** Active managers in a company. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.company = :company AND u.role = 'ROLE_MANAGER' ORDER BY u.firstName")
    List<User> findManagersByCompany(@Param("company") Company company);

    /** Active employees in a department. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.department = :department AND u.role = 'ROLE_EMPLOYEE' ORDER BY u.firstName")
    List<User> findEmployeesByDepartment(@Param("department") Department department);

    /** Active employees in a company. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.company = :company AND u.role = 'ROLE_EMPLOYEE' ORDER BY u.firstName")
    List<User> findEmployeesByCompany(@Param("company") Company company);

    /** Soft-deleted users for audit trail. */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL ORDER BY u.deletedAt DESC")
    List<User> findAllDeleted();

    /** All active users (for site admin). */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    List<User> findAllActive();
}
