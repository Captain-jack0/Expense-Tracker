package com.expensetracker.service;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.Bucket;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryKind;
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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private SubCategoryRepository subCategoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CategorySeeder categorySeeder;

    private CategoryService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, subCategoryRepository,
                transactionRepository, categorySeeder);
    }

    private Category category(String name, boolean system) {
        return Category.builder()
                .id(UUID.randomUUID()).userId(userId).name(name)
                .kind(CategoryKind.EXPENSE).bucket(Bucket.NEEDS)
                .system(system).sortOrder(0).build();
    }

    private CategoryRequest request(String name) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name);
        r.setKind(CategoryKind.EXPENSE);
        r.setBucket(Bucket.WANTS);
        return r;
    }

    @Test
    @DisplayName("list seeds defaults on first access, then returns categories")
    void list_seedsWhenEmpty() {
        when(categoryRepository.countByUserId(userId)).thenReturn(0L);
        when(categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId))
                .thenReturn(List.of(category("Needs", true)));

        List<CategoryDto> result = service.list(userId);

        verify(categorySeeder).seedFor(userId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Needs");
    }

    @Test
    @DisplayName("list does not seed when the user already has categories")
    void list_noSeedWhenPresent() {
        when(categoryRepository.countByUserId(userId)).thenReturn(3L);
        when(categoryRepository.findByUserIdOrderBySortOrderAscNameAsc(userId)).thenReturn(List.of());

        service.list(userId);
        verify(categorySeeder, never()).seedFor(any());
    }

    @Test
    @DisplayName("create saves a new category with the next sort order")
    void create_success() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Travel")).thenReturn(false);
        when(categoryRepository.countByUserId(userId)).thenReturn(4L);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CategoryDto dto = service.create(userId, request("  Travel  "));
        assertThat(dto.getName()).isEqualTo("Travel");
        assertThat(dto.getSortOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("create rejects a duplicate name with 409")
    void create_duplicate_conflict() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Food")).thenReturn(true);
        assertThatThrownBy(() -> service.create(userId, request("Food")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update rejects modifying a system category with 403")
    void update_systemForbidden() {
        Category system = category("Income", true);
        when(categoryRepository.findByIdAndUserId(system.getId(), userId)).thenReturn(Optional.of(system));
        assertThatThrownBy(() -> service.update(userId, system.getId(), request("Renamed")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("update fails with 404 for an unknown category")
    void update_notFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(userId, id, request("X")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("update saves changes to a user category")
    void update_success() {
        Category c = category("Old", false);
        when(categoryRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "New")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto dto = service.update(userId, c.getId(), request("New"));
        assertThat(dto.getName()).isEqualTo("New");
        assertThat(dto.getBucket()).isEqualTo(Bucket.WANTS);
    }

    @Test
    @DisplayName("delete rejects a category that still has transactions with 409")
    void delete_hasTransactions_conflict() {
        Category c = category("Food", false);
        when(categoryRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(transactionRepository.existsByCategoryId(c.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.delete(userId, c.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("delete removes an empty user category and its sub-categories")
    void delete_success() {
        Category c = category("Food", false);
        when(categoryRepository.findByIdAndUserId(c.getId(), userId)).thenReturn(Optional.of(c));
        when(transactionRepository.existsByCategoryId(c.getId())).thenReturn(false);
        when(subCategoryRepository.findByCategoryIdOrderByNameAsc(c.getId())).thenReturn(List.of());

        service.delete(userId, c.getId());
        verify(categoryRepository).delete(c);
    }

    @Test
    @DisplayName("delete rejects a system category with 403")
    void delete_systemForbidden() {
        Category system = category("Income", true);
        when(categoryRepository.findByIdAndUserId(system.getId(), userId)).thenReturn(Optional.of(system));
        assertThatThrownBy(() -> service.delete(userId, system.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
