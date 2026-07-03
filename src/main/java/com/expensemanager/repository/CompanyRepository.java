package com.expensemanager.repository;

import com.expensemanager.model.Company;
import com.expensemanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCeo(User ceo);
    List<Company> findAllByActiveTrue();
    boolean existsByName(String name);
}
