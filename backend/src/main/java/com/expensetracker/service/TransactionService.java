package com.expensetracker.service;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.Category;
import com.expensetracker.model.SourceType;
import com.expensetracker.model.SubCategory;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt");

    /** Upper bound for the paginated endpoint's {@code limit}. */
    public static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              SubCategoryRepository subCategoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> list(UUID userId, UUID categoryId, UUID subCategoryId,
                                     TransactionType type, LocalDate from, LocalDate to) {
        return transactionRepository
                .search(userId, categoryId, subCategoryId, type, from, to, Pageable.unpaged(DEFAULT_SORT))
                .stream()
                .map(TransactionDto::from)
                .toList();
    }

    /** One page of the user's transactions (1-based {@code page}), newest first. */
    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> listPage(UUID userId, UUID categoryId, UUID subCategoryId,
                                                 TransactionType type, LocalDate from, LocalDate to,
                                                 int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        Page<Transaction> result = transactionRepository.search(userId, categoryId, subCategoryId, type,
                from, to, PageRequest.of(safePage - 1, safeLimit, DEFAULT_SORT));
        return PageResponse.of(result.map(TransactionDto::from), safePage, safeLimit);
    }

    @Transactional(readOnly = true)
    public TransactionDto get(UUID userId, UUID id) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .map(TransactionDto::from)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
    }

    @Transactional
    public TransactionDto create(UUID userId, TransactionRequest request) {
        validateCategoryAndSubCategory(userId, request.getCategoryId(), request.getSubCategoryId(), request.getType());
        Transaction saved = transactionRepository.save(Transaction.builder()
                .userId(userId)
                .name(request.getName().trim())
                .amount(request.getAmount())
                .currency(normaliseCurrency(request.getCurrency()))
                .categoryId(request.getCategoryId())
                .subCategoryId(request.getSubCategoryId())
                .type(request.getType())
                .transactionDate(request.getTransactionDate())
                .note(request.getNote())
                .sourceType(SourceType.MANUAL)
                .build());
        return TransactionDto.from(saved);
    }

    @Transactional
    public TransactionDto update(UUID userId, UUID id, TransactionRequest request) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        validateCategoryAndSubCategory(userId, request.getCategoryId(), request.getSubCategoryId(), request.getType());
        tx.setName(request.getName().trim());
        tx.setAmount(request.getAmount());
        tx.setCurrency(normaliseCurrency(request.getCurrency()));
        tx.setCategoryId(request.getCategoryId());
        tx.setSubCategoryId(request.getSubCategoryId());
        tx.setType(request.getType());
        tx.setTransactionDate(request.getTransactionDate());
        tx.setNote(request.getNote());
        return TransactionDto.from(transactionRepository.save(tx));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        transactionRepository.delete(tx);
    }

    /**
     * Enforces the core entry rule: the category must be owned by the user,
     * its kind must match the transaction type (no EXPENSE under an INCOME
     * category and vice versa), a sub-category is required when the category
     * has any, and the chosen sub-category must belong to that category.
     */
    private void validateCategoryAndSubCategory(UUID userId, UUID categoryId, UUID subCategoryId,
                                                TransactionType type) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (category.getKind() != null && type != null && !category.getKind().name().equals(type.name())) {
            throw ApiException.badRequest("A " + type + " transaction cannot use the "
                    + category.getKind() + " category '" + category.getName() + "'");
        }

        boolean categoryHasSubs = subCategoryRepository.countByCategoryId(categoryId) > 0;

        if (subCategoryId == null) {
            if (categoryHasSubs) {
                throw ApiException.badRequest("A sub-category is required for this category");
            }
            return;
        }

        SubCategory sub = subCategoryRepository.findByIdAndUserId(subCategoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Sub-category not found"));
        if (!sub.getCategoryId().equals(categoryId)) {
            throw ApiException.badRequest("Sub-category does not belong to the selected category");
        }
    }

    private String normaliseCurrency(String currency) {
        return (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
    }
}
