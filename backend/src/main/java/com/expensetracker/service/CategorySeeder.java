package com.expensetracker.service;

import com.expensetracker.model.Bucket;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryKind;
import com.expensetracker.model.SubCategory;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a new user's default categories and sub-categories (50/30/20 layout).
 * Seeds are user-owned and fully editable (isSystem=false) in P1.
 */
@Component
public class CategorySeeder {

    private record Seed(String name, CategoryKind kind, Bucket bucket, String color, List<String> subs) {}

    private static final List<Seed> DEFAULTS = List.of(
            new Seed("Income", CategoryKind.INCOME, Bucket.INCOME, "#22c55e",
                    List.of("Salary", "Bonus", "Other Income")),
            new Seed("Needs", CategoryKind.EXPENSE, Bucket.NEEDS, "#ef4444",
                    List.of("Rent", "Groceries", "Utilities", "Transport", "Health")),
            new Seed("Wants", CategoryKind.EXPENSE, Bucket.WANTS, "#f59e0b",
                    List.of("Dining", "Entertainment", "Shopping")),
            new Seed("Savings", CategoryKind.EXPENSE, Bucket.SAVINGS, "#3b82f6",
                    List.of("Emergency Fund", "Investment")),
            new Seed("Other", CategoryKind.EXPENSE, Bucket.OTHER, "#64748b",
                    List.of())
    );

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository,
                          SubCategoryRepository subCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public void seedFor(UUID userId) {
        int sortOrder = 0;
        for (Seed seed : DEFAULTS) {
            Category category = categoryRepository.save(Category.builder()
                    .userId(userId)
                    .name(seed.name())
                    .kind(seed.kind())
                    .bucket(seed.bucket())
                    .color(seed.color())
                    .system(false)
                    .sortOrder(sortOrder++)
                    .build());

            for (String subName : seed.subs()) {
                subCategoryRepository.save(SubCategory.builder()
                        .categoryId(category.getId())
                        .userId(userId)
                        .name(subName)
                        .system(false)
                        .build());
            }
        }
    }
}
