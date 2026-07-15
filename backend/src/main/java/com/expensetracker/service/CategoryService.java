package com.expensetracker.service;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.Category;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategorySeeder categorySeeder;

    public CategoryService(CategoryRepository categoryRepository,
                           SubCategoryRepository subCategoryRepository,
                           TransactionRepository transactionRepository,
                           CategorySeeder categorySeeder) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.transactionRepository = transactionRepository;
        this.categorySeeder = categorySeeder;
    }

    /** Lists the user's categories, seeding sensible defaults on first access. */
    @Transactional
    public List<CategoryDto> list(UUID userId) {
        if (categoryRepository.countByUserId(userId) == 0) {
            categorySeeder.seedFor(userId);
        }
        return categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId).stream()
                .map(CategoryDto::from)
                .toList();
    }

    @Transactional
    public CategoryDto create(UUID userId, CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw ApiException.conflict("A category with this name already exists");
        }
        int nextSort = (int) categoryRepository.countByUserId(userId);
        Category saved = categoryRepository.save(Category.builder()
                .userId(userId)
                .name(name)
                .kind(request.getKind())
                .bucket(request.getBucket())
                .icon(request.getIcon())
                .color(request.getColor())
                .system(false)
                .sortOrder(nextSort)
                .build());
        return CategoryDto.from(saved);
    }

    @Transactional
    public CategoryDto update(UUID userId, UUID id, CategoryRequest request) {
        Category category = requireOwned(userId, id);
        if (category.isSystem()) {
            throw ApiException.forbidden("System categories cannot be modified");
        }
        String name = request.getName().trim();
        if (!name.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw ApiException.conflict("A category with this name already exists");
        }
        category.setName(name);
        category.setKind(request.getKind());
        category.setBucket(request.getBucket());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        return CategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Category category = requireOwned(userId, id);
        if (category.isSystem()) {
            throw ApiException.forbidden("System categories cannot be deleted");
        }
        if (transactionRepository.existsByCategoryId(id)) {
            throw ApiException.conflict("Category has transactions; reassign or delete them first");
        }
        subCategoryRepository.deleteAll(subCategoryRepository.findByCategoryIdOrderByNameAsc(id));
        categoryRepository.delete(category);
    }

    private Category requireOwned(UUID userId, UUID id) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
    }
}
