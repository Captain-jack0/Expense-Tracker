package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> list(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.list(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto>> create(@CurrentUser UUID userId,
                                                           @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.create(userId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> update(@CurrentUser UUID userId,
                                                           @PathVariable UUID id,
                                                           @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        categoryService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted"));
    }
}
