package com.expensemanager.service;

import com.expensemanager.dto.ExpenseDTO;
import com.expensemanager.dto.ExpenseSearchDTO;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExpenseService {
    Expense create(ExpenseDTO dto, User user);
    Expense update(Long id, ExpenseDTO dto, User user);
    Expense findById(Long id);
    Page<Expense> search(ExpenseSearchDTO searchDTO, User user);
    Page<Expense> findAll(ExpenseSearchDTO searchDTO);
    void submit(Long id, User user);
    void approve(Long id, User approver);
    void reject(Long id, User approver, String reason);
    void softDelete(Long id, User user);
    long countDraftsByUser(User user);
}
