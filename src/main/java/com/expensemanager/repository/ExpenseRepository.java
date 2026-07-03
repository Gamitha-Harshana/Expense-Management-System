package com.expensemanager.repository;

import com.expensemanager.model.Company;
import com.expensemanager.model.Department;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseCategory;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.model.enums.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.user = :user ORDER BY e.createdAt DESC")
    Page<Expense> findByUserAndNotDeleted(@Param("user") User user, Pageable pageable);

    /** All expenses for a company (CEO view). */
    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.user.company = :company ORDER BY e.createdAt DESC")
    Page<Expense> findByCompany(@Param("company") Company company, Pageable pageable);

    /** All expenses for a department (manager view). */
    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.user.department = :department ORDER BY e.createdAt DESC")
    Page<Expense> findByDepartment(@Param("department") Department department, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NOT NULL ORDER BY e.deletedAt DESC")
    List<Expense> findAllDeleted();

    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.status = :status ORDER BY e.submittedAt DESC")
    List<Expense> findByStatus(@Param("status") ExpenseStatus status);

    /** Pending approvals scoped to a company. */
    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.user.company = :company AND e.status = 'SUBMITTED' ORDER BY e.submittedAt DESC")
    List<Expense> findPendingByCompany(@Param("company") Company company);

    /** Pending approvals scoped to a department. */
    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL AND e.user.department = :department AND e.status = 'SUBMITTED' ORDER BY e.submittedAt DESC")
    List<Expense> findPendingByDepartment(@Param("department") Department department);

    /** Search with optional company and department scope. */
    @Query("SELECT e FROM Expense e WHERE e.deletedAt IS NULL " +
           "AND (:user IS NULL OR e.user = :user) " +
           "AND (:company IS NULL OR e.user.company = :company) " +
           "AND (:department IS NULL OR e.user.department = :department) " +
           "AND (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:expenseType IS NULL OR e.expenseType = :expenseType) " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount) " +
           "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
           "AND (:toDate IS NULL OR e.expenseDate <= :toDate) " +
           "ORDER BY e.createdAt DESC")
    Page<Expense> searchExpenses(
            @Param("user") User user,
            @Param("company") Company company,
            @Param("department") Department department,
            @Param("keyword") String keyword,
            @Param("category") ExpenseCategory category,
            @Param("status") ExpenseStatus status,
            @Param("expenseType") ExpenseType expenseType,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.deletedAt IS NULL AND e.user = :user AND e.category = :category AND MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year AND e.status != 'REJECTED'")
    BigDecimal sumByUserCategoryMonthYear(@Param("user") User user, @Param("category") ExpenseCategory category, @Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.deletedAt IS NULL AND e.user = :user AND e.status = 'DRAFT'")
    long countDraftsByUser(@Param("user") User user);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.deletedAt IS NULL AND e.user.company = :company AND e.status = 'SUBMITTED'")
    long countPendingByCompany(@Param("company") Company company);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.deletedAt IS NULL AND e.user.department = :department AND e.status = 'SUBMITTED'")
    long countPendingByDepartment(@Param("department") Department department);
}
