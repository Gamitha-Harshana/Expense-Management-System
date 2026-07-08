package com.expensemanager.repository;

import com.expensemanager.model.ExpenseReport;
import com.expensemanager.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    @Query("SELECT r FROM ExpenseReport r WHERE r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<ExpenseReport> findAllActive(Pageable pageable);

    @Query("SELECT r FROM ExpenseReport r WHERE r.deletedAt IS NULL AND r.submittedBy = :user ORDER BY r.createdAt DESC")
    Page<ExpenseReport> findBySubmittedBy(@Param("user") User user, Pageable pageable);

    @Query("SELECT r FROM ExpenseReport r WHERE r.deletedAt IS NOT NULL ORDER BY r.deletedAt DESC")
    List<ExpenseReport> findAllDeleted();

    Optional<ExpenseReport> findByIdAndDeletedAtIsNull(Long id);
}
