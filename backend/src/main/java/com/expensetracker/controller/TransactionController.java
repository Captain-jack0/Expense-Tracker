package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.model.TransactionType;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDto>>> list(
            @CurrentUser UUID userId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "subCategoryId", required = false) UUID subCategoryId,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.list(userId, categoryId, subCategoryId, type, from, to)));
    }

    /**
     * Paginated variant of {@link #list}. {@code page} is 1-based; {@code limit}
     * is clamped to 1..{@value TransactionService#MAX_PAGE_SIZE}.
     */
    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<TransactionDto>> listPaginated(
            @CurrentUser UUID userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "subCategoryId", required = false) UUID subCategoryId,
            @RequestParam(value = "type", required = false) TransactionType type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.listPage(
                userId, categoryId, subCategoryId, type, from, to, page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.get(userId, id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDto>> create(@CurrentUser UUID userId,
                                                              @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(transactionService.create(userId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> update(@CurrentUser UUID userId,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.update(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        transactionService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Transaction deleted"));
    }
}
