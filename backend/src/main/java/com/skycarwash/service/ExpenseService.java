package com.skycarwash.service;

import com.skycarwash.dto.ExpenseRequest;
import com.skycarwash.dto.ExpenseResponse;
import com.skycarwash.entity.Expense;
import com.skycarwash.entity.User;
import com.skycarwash.exception.BusinessException;
import com.skycarwash.repository.ExpenseRepository;
import com.skycarwash.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository    userRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByMonth(YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate to   = yearMonth.atEndOfMonth();
        return expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest req, String createdByPhone) {
        Expense.Category category;
        try {
            category = Expense.Category.valueOf(req.category().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Catégorie invalide : " + req.category());
        }

        User user = userRepository.findByPhone(createdByPhone)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + createdByPhone));

        Expense expense = Expense.builder()
                .category(category)
                .amount(req.amount())
                .description(req.description())
                .expenseDate(req.expenseDate() != null ? req.expenseDate() : LocalDate.now())
                .createdBy(user)
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + id));
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getCategory().name(),
                e.getAmount(),
                e.getDescription(),
                e.getExpenseDate(),
                e.getCreatedBy().getName(),
                e.getCreatedAt()
        );
    }
}
