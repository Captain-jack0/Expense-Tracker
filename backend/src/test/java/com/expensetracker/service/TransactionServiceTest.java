package com.expensetracker.service;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.exception.ApiException;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryKind;
import com.expensetracker.model.SubCategory;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.SubCategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubCategoryRepository subCategoryRepository;

    private TransactionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID subCategoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionService(transactionRepository, categoryRepository, subCategoryRepository);
    }

    private TransactionRequest request(UUID subId, String currency) {
        TransactionRequest r = new TransactionRequest();
        r.setName("  Groceries  ");
        r.setAmount(new BigDecimal("42.50"));
        r.setCurrency(currency);
        r.setCategoryId(categoryId);
        r.setSubCategoryId(subId);
        r.setType(TransactionType.EXPENSE);
        r.setTransactionDate(LocalDate.of(2026, 7, 15));
        r.setNote("note");
        return r;
    }

    /** save() echoes the entity back with id + timestamps, mimicking JPA. */
    private void stubSaveEchoes() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });
    }

    @Test
    @DisplayName("create succeeds for a category without sub-categories")
    void create_noSubs_success() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(0L);
        stubSaveEchoes();

        TransactionDto dto = service.create(userId, request(null, "usd"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Groceries");           // trimmed
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");             // normalised
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(dto.getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("create defaults a blank currency to USD")
    void create_blankCurrency_defaultsUsd() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(0L);
        stubSaveEchoes();

        service.create(userId, request(null, null));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("create fails with 404 when the category isn't owned")
    void create_categoryNotFound() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, request(null, "USD")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create fails with 400 when a sub-category is required but missing")
    void create_subRequiredButMissing() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(2L);

        assertThatThrownBy(() -> service.create(userId, request(null, "USD")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("create fails with 404 when the given sub-category isn't owned")
    void create_subNotFound() {
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(1L);
        when(subCategoryRepository.findByIdAndUserId(subCategoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, request(subCategoryId, "USD")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("create fails with 400 when the sub-category belongs to another category")
    void create_subWrongCategory() {
        SubCategory sub = SubCategory.builder().id(subCategoryId).categoryId(UUID.randomUUID()).build();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(1L);
        when(subCategoryRepository.findByIdAndUserId(subCategoryId, userId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.create(userId, request(subCategoryId, "USD")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("create succeeds with a valid sub-category")
    void create_withValidSub_success() {
        SubCategory sub = SubCategory.builder().id(subCategoryId).categoryId(categoryId).build();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(1L);
        when(subCategoryRepository.findByIdAndUserId(subCategoryId, userId)).thenReturn(Optional.of(sub));
        stubSaveEchoes();

        TransactionDto dto = service.create(userId, request(subCategoryId, "USD"));
        assertThat(dto.getSubCategoryId()).isEqualTo(subCategoryId.toString());
    }

    @Test
    @DisplayName("update mutates an owned transaction")
    void update_success() {
        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID()).userId(userId).categoryId(categoryId)
                .type(TransactionType.EXPENSE).transactionDate(LocalDate.now())
                .amount(BigDecimal.ONE).currency("USD").build();
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        when(transactionRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(new Category()));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(0L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionDto dto = service.update(userId, existing.getId(), request(null, "USD"));
        assertThat(dto.getName()).isEqualTo("Groceries");
        assertThat(dto.getAmount()).isEqualByComparingTo("42.50");
    }

    @Test
    @DisplayName("update fails with 404 for an unknown transaction")
    void update_notFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(userId, id, request(null, "USD")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("delete removes an owned transaction")
    void delete_success() {
        Transaction existing = Transaction.builder().id(UUID.randomUUID()).userId(userId).build();
        when(transactionRepository.findByIdAndUserId(existing.getId(), userId)).thenReturn(Optional.of(existing));

        service.delete(userId, existing.getId());
        verify(transactionRepository).delete(existing);
    }

    @Test
    @DisplayName("delete fails with 404 for an unknown transaction")
    void delete_notFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(userId, id)).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("create fails with 400 when the transaction type doesn't match the category kind")
    void create_typeKindMismatch() {
        Category income = Category.builder().id(categoryId).userId(userId).name("Income")
                .kind(CategoryKind.INCOME).build();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(income));

        assertThatThrownBy(() -> service.create(userId, request(null, "USD")))   // request() is EXPENSE
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("EXPENSE transaction cannot use the INCOME category 'Income'")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create succeeds when the transaction type matches the category kind")
    void create_typeKindMatch() {
        Category needs = Category.builder().id(categoryId).userId(userId).name("Needs")
                .kind(CategoryKind.EXPENSE).build();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(needs));
        when(subCategoryRepository.countByCategoryId(categoryId)).thenReturn(0L);
        stubSaveEchoes();

        assertThat(service.create(userId, request(null, "USD")).getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("get returns an owned transaction, else 404")
    void get_ownedOrNotFound() {
        Transaction t = Transaction.builder()
                .id(UUID.randomUUID()).userId(userId).name("Rent").categoryId(categoryId)
                .type(TransactionType.EXPENSE).transactionDate(LocalDate.now())
                .amount(BigDecimal.TEN).currency("USD").build();
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        when(transactionRepository.findByIdAndUserId(t.getId(), userId)).thenReturn(Optional.of(t));
        assertThat(service.get(userId, t.getId()).getName()).isEqualTo("Rent");

        UUID missing = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(missing, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(userId, missing))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("listPage converts the 1-based page, clamps the limit and reports totals")
    void listPage_clampsAndReports() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(transactionRepository.search(any(), any(), any(), any(), any(), any(), pageable.capture()))
                .thenAnswer(inv -> new PageImpl<Transaction>(List.of(), inv.getArgument(6), 250));

        PageResponse<TransactionDto> result = service.listPage(userId, null, null, null, null, null, 3, 1000);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);                       // 0-based
        assertThat(pageable.getValue().getPageSize()).isEqualTo(TransactionService.MAX_PAGE_SIZE);
        assertThat(result.pagination().page()).isEqualTo(3);
        assertThat(result.pagination().limit()).isEqualTo(TransactionService.MAX_PAGE_SIZE);
        assertThat(result.pagination().total()).isEqualTo(250);
        assertThat(result.pagination().totalPages()).isEqualTo(3);
        assertThat(result.success()).isTrue();

        service.listPage(userId, null, null, null, null, null, 0, 0);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("list maps the searched transactions to DTOs")
    void list_mapsResults() {
        Transaction t = Transaction.builder()
                .id(UUID.randomUUID()).userId(userId).name("Rent").categoryId(categoryId)
                .type(TransactionType.EXPENSE).transactionDate(LocalDate.now())
                .amount(new BigDecimal("1000")).currency("USD").build();
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        Page<Transaction> page = new PageImpl<>(List.of(t));
        when(transactionRepository.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        List<TransactionDto> result = service.list(userId, null, null, null, null, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Rent");
    }
}
