package com.expensemanager.service.impl;

import com.expensemanager.dto.ExpenseDTO;
import com.expensemanager.dto.ExpenseSearchDTO;
import com.expensemanager.exception.ResourceNotFoundException;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import com.expensemanager.model.enums.ExpenseStatus;
import com.expensemanager.model.enums.Role;
import com.expensemanager.repository.ExpenseRepository;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.service.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final BudgetService budgetService;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              NotificationService notificationService,
                              EmailService emailService,
                              BudgetService budgetService) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.budgetService = budgetService;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true),
            @CacheEvict(value = "userExpenseSummary", allEntries = true)
    })
    public Expense create(ExpenseDTO dto, User user) {
        Expense expense = new Expense();
        mapDtoToExpense(dto, expense);
        expense.setUser(user);
        expense.setStatus(ExpenseStatus.DRAFT);

        handleReceiptUpload(dto.getReceiptFile(), expense);

        Expense saved = expenseRepository.save(expense);

        // Update budget spending
        budgetService.updateSpending(user, expense.getCategory(), expense.getAmount(), expense.getExpenseDate());

        return saved;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true),
            @CacheEvict(value = "userExpenseSummary", allEntries = true)
    })
    public Expense update(Long id, ExpenseDTO dto, User user) {
        Expense expense = findById(id);
        if (!expense.getUser().getId().equals(user.getId()) &&
                user.getRole() != Role.ROLE_CEO && user.getRole() != Role.ROLE_MANAGER) {
            throw new AccessDeniedException("You cannot edit this expense");
        }
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT expenses can be edited");
        }
        mapDtoToExpense(dto, expense);
        handleReceiptUpload(dto.getReceiptFile(), expense);
        return expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Expense findById(Long id) {
        return expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> search(ExpenseSearchDTO dto, User user) {
        Pageable pageable = buildPageable(dto);
        return expenseRepository.searchExpenses(
                user,
                null,
                null,
                dto.getKeyword() != null && !dto.getKeyword().isBlank() ? dto.getKeyword() : null,
                dto.getCategory(), dto.getStatus(), dto.getExpenseType(),
                dto.getMinAmount(), dto.getMaxAmount(),
                dto.getFromDate(), dto.getToDate(),
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> findAll(ExpenseSearchDTO dto) {
        Pageable pageable = buildPageable(dto);
        return expenseRepository.searchExpenses(
                null,
                null,
                null,
                dto.getKeyword() != null && !dto.getKeyword().isBlank() ? dto.getKeyword() : null,
                dto.getCategory(), dto.getStatus(), dto.getExpenseType(),
                dto.getMinAmount(), dto.getMaxAmount(),
                dto.getFromDate(), dto.getToDate(),
                pageable);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true)
    })
    public void submit(Long id, User user) {
        Expense expense = findById(id);
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot submit this expense");
        }
        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT expenses can be submitted");
        }
        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        // Notify CEO and Managers in the company
        List<User> managers = userRepository.findManagersByCompany(user.getCompany());
        managers.forEach(manager -> {
            notificationService.create(manager,
                    "New expense pending approval",
                    user.getFullName() + " submitted: " + expense.getTitle(),
                    com.expensemanager.model.enums.NotificationType.INFO,
                    "/expenses/" + id);
            emailService.sendExpenseSubmittedNotification(manager.getEmail(),
                    user.getFullName(), expense.getTitle(), id);
        });
        
        User ceo = user.getCompany() != null ? user.getCompany().getCeo() : null;
        if (ceo != null && !managers.contains(ceo)) {
            notificationService.create(ceo,
                    "New expense pending approval",
                    user.getFullName() + " submitted: " + expense.getTitle(),
                    com.expensemanager.model.enums.NotificationType.INFO,
                    "/expenses/" + id);
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true)
    })
    public void approve(Long id, User approver) {
        Expense expense = findById(id);
        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED expenses can be approved");
        }
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprover(approver);
        expense.setApprovedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        User owner = expense.getUser();
        notificationService.create(owner,
                "Expense approved \u2705",
                "Your expense '" + expense.getTitle() + "' was approved by " + approver.getFullName(),
                com.expensemanager.model.enums.NotificationType.SUCCESS,
                "/expenses/" + id);
        emailService.sendExpenseApprovedNotification(owner.getEmail(), expense.getTitle());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true)
    })
    public void reject(Long id, User approver, String reason) {
        Expense expense = findById(id);
        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED expenses can be rejected");
        }
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setApprover(approver);
        expense.setRejectionReason(reason);
        expenseRepository.save(expense);

        User owner = expense.getUser();
        notificationService.create(owner,
                "Expense rejected \u274c",
                "Your expense '" + expense.getTitle() + "' was rejected. Reason: " + reason,
                com.expensemanager.model.enums.NotificationType.ERROR,
                "/expenses/" + id);
        emailService.sendExpenseRejectedNotification(owner.getEmail(), expense.getTitle(), reason);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboardStats", allEntries = true),
            @CacheEvict(value = "userExpenseSummary", allEntries = true)
    })
    public void softDelete(Long id, User user) {
        Expense expense = findById(id);
        if (!expense.getUser().getId().equals(user.getId()) &&
                user.getRole() != Role.ROLE_CEO && user.getRole() != Role.ROLE_MANAGER) {
            throw new AccessDeniedException("You cannot delete this expense");
        }
        expense.setDeletedAt(LocalDateTime.now());
        expenseRepository.save(expense);
        // Keep budget in sync — recalculate actual spend after the expense is removed
        budgetService.recalculateSpend(expense.getUser(), expense.getCategory(), expense.getExpenseDate());
    }


    @Override
    @Transactional(readOnly = true)
    public long countDraftsByUser(User user) {
        return expenseRepository.countDraftsByUser(user);
    }

    private void mapDtoToExpense(ExpenseDTO dto, Expense expense) {
        expense.setTitle(dto.getTitle());
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
        expense.setCategory(dto.getCategory());
        expense.setExpenseType(dto.getExpenseType());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setNotes(dto.getNotes());
    }

    private void handleReceiptUpload(MultipartFile file, Expense expense) {
        if (file != null && !file.isEmpty()) {
            try {
                String storedName = fileStorageService.storeFile(file);
                expense.setReceiptPath(storedName);
                expense.setReceiptFileName(file.getOriginalFilename());
                expense.setReceiptContentType(file.getContentType());
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload receipt: " + e.getMessage());
            }
        }
    }

    private Pageable buildPageable(ExpenseSearchDTO dto) {
        Sort sort = dto.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(dto.getSortBy()).ascending()
                : Sort.by(dto.getSortBy()).descending();
        return PageRequest.of(dto.getPage(), dto.getSize(), sort);
    }
}
