package com.expensemanager.repository;

import com.expensemanager.model.EmployeeBudget;
import com.expensemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeBudgetRepository extends JpaRepository<EmployeeBudget, Long> {

    /** All pools for an employee, newest first. */
    List<EmployeeBudget> findByEmployeeOrderByStartDateDesc(User employee);

    /** Active pools for an employee today. */
    @Query("""
        SELECT b FROM EmployeeBudget b
        WHERE b.employee = :employee
          AND b.startDate <= :today
          AND b.endDate   >= :today
        ORDER BY b.startDate DESC
        """)
    List<EmployeeBudget> findActiveByEmployee(
            @Param("employee") User employee,
            @Param("today") LocalDate today);

    /** All pools assigned by a specific manager. */
    List<EmployeeBudget> findByAssignedByOrderByStartDateDesc(User manager);

    /** All pools for employees in a company (via manager's company). */
    @Query("""
        SELECT b FROM EmployeeBudget b
        WHERE b.assignedBy.company.id = :companyId
        ORDER BY b.startDate DESC
        """)
    List<EmployeeBudget> findByCompanyId(@Param("companyId") Long companyId);
}
