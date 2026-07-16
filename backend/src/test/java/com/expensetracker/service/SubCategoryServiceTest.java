package com.expensetracker.service;

import com.expensetracker.dto.SubCategoryDto;
import com.expensetracker.dto.SubCategoryRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.Category;
import com.expensetracker.model.SubCategory;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubCategoryServiceTest {

    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;

    private SubCategoryService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubCategoryService(subCategoryRepository, categoryRepository, transactionRepository);
    }

    private Category ownedCategory() {
        return Category.builder().id(categoryId).userId(userId).build();
    }

    private SubCategory sub(String name) {
        return SubCategory.builder()
                .id(UUID.randomUUID()).categoryId(categoryId).userId(userId).name(name).build();
    }

    private SubCategoryRequest request(String name, BigDecimal limit) {
        SubCategoryRequest r = new SubCategoryRequest();
        r.setCategoryId(categoryId);
        r.setName(name);
        r.setMonthlyLimit(limit);
        return r;
    }

    @Test
    @DisplayName("list by category returns that category's sub-categories")
    void list_byCategory() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(ownedCategory()));
        when(subCategoryRepository.findByCategoryIdOrderByNameAsc(categoryId))
                .thenReturn(List.of(sub("Groceries")));

        List<SubCategoryDto> result = service.list(userId, categoryId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("list by an unowned category fails with 404")
    void list_categoryNotFound() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(userId, categoryId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("list without a category returns all the user's sub-categories")
    void list_allForUser() {
        when(subCategoryRepository.findByUserIdOrderByNameAsc(userId)).thenReturn(List.of(sub("A"), sub("B")));
        assertThat(service.list(userId, null)).hasSize(2);
    }

    @Test
    @DisplayName("create saves a new sub-category under an owned category")
    void create_success() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(ownedCategory()));
        when(subCategoryRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Groceries")).thenReturn(false);
        when(subCategoryRepository.save(any(SubCategory.class))).thenAnswer(inv -> {
            SubCategory s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubCategoryDto dto = service.create(userId, request("  Groceries  ", new BigDecimal("600")));
        assertThat(dto.getName()).isEqualTo("Groceries");
        assertThat(dto.getMonthlyLimit()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("create under an unowned category fails with 404")
    void create_categoryNotFound() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(userId, request("X", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(subCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rejects a duplicate name within the category with 409")
    void create_duplicate_conflict() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(ownedCategory()));
        when(subCategoryRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "Groceries")).thenReturn(true);
        assertThatThrownBy(() -> service.create(userId, request("Groceries", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("update fails with 404 for an unknown sub-category")
    void update_notFound() {
        UUID id = UUID.randomUUID();
        when(subCategoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(userId, id, request("X", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("update renames an owned sub-category")
    void update_success() {
        SubCategory existing = sub("Old");
        when(subCategoryRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(ownedCategory()));
        when(subCategoryRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, "New")).thenReturn(false);
        when(subCategoryRepository.save(any(SubCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        SubCategoryDto dto = service.update(userId, existing.getId(), request("New", null));
        assertThat(dto.getName()).isEqualTo("New");
    }

    @Test
    @DisplayName("delete rejects a sub-category that still has transactions with 409")
    void delete_hasTransactions_conflict() {
        SubCategory existing = sub("Groceries");
        when(subCategoryRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsBySubCategoryId(existing.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.delete(userId, existing.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("delete removes an unused sub-category")
    void delete_success() {
        SubCategory existing = sub("Groceries");
        when(subCategoryRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(transactionRepository.existsBySubCategoryId(existing.getId())).thenReturn(false);

        service.delete(userId, existing.getId());
        verify(subCategoryRepository).delete(existing);
    }
}
