package com.expensetracker.service;

import com.expensetracker.dto.SubCategoryDto;
import com.expensetracker.dto.SubCategoryRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.SubCategory;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public SubCategoryService(SubCategoryRepository subCategoryRepository,
                              CategoryRepository categoryRepository,
                              TransactionRepository transactionRepository) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<SubCategoryDto> list(UUID userId, UUID categoryId) {
        List<SubCategory> subs = (categoryId != null)
                ? subCategoryRepository.findByCategoryIdOrderByNameAsc(requireOwnedCategory(userId, categoryId))
                : subCategoryRepository.findByUserIdOrderByNameAsc(userId);
        return subs.stream().map(SubCategoryDto::from).toList();
    }

    @Transactional
    public SubCategoryDto create(UUID userId, SubCategoryRequest request) {
        UUID categoryId = requireOwnedCategory(userId, request.getCategoryId());
        String name = request.getName().trim();
        if (subCategoryRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name)) {
            throw ApiException.conflict("A sub-category with this name already exists in this category");
        }
        SubCategory saved = subCategoryRepository.save(SubCategory.builder()
                .categoryId(categoryId)
                .userId(userId)
                .name(name)
                .icon(request.getIcon())
                .color(request.getColor())
                .monthlyLimit(request.getMonthlyLimit())
                .system(false)
                .build());
        return SubCategoryDto.from(saved);
    }

    @Transactional
    public SubCategoryDto update(UUID userId, UUID id, SubCategoryRequest request) {
        SubCategory sub = requireOwned(userId, id);
        UUID targetCategoryId = requireOwnedCategory(userId, request.getCategoryId());
        String name = request.getName().trim();

        boolean nameOrCategoryChanged = !targetCategoryId.equals(sub.getCategoryId())
                || !name.equalsIgnoreCase(sub.getName());
        if (nameOrCategoryChanged
                && subCategoryRepository.existsByCategoryIdAndNameIgnoreCase(targetCategoryId, name)) {
            throw ApiException.conflict("A sub-category with this name already exists in this category");
        }

        sub.setCategoryId(targetCategoryId);
        sub.setName(name);
        sub.setIcon(request.getIcon());
        sub.setColor(request.getColor());
        sub.setMonthlyLimit(request.getMonthlyLimit());
        return SubCategoryDto.from(subCategoryRepository.save(sub));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        SubCategory sub = requireOwned(userId, id);
        if (transactionRepository.existsBySubCategoryId(id)) {
            throw ApiException.conflict("Sub-category has transactions; reassign or delete them first");
        }
        subCategoryRepository.delete(sub);
    }

    private SubCategory requireOwned(UUID userId, UUID id) {
        return subCategoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Sub-category not found"));
    }

    /** Verifies the category exists and belongs to the user; returns its id. */
    private UUID requireOwnedCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"))
                .getId();
    }
}
