package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.SubCategoryDto;
import com.expensetracker.dto.SubCategoryRequest;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.SubCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subcategories")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    public SubCategoryController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubCategoryDto>>> list(
            @CurrentUser UUID userId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(subCategoryService.list(userId, categoryId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubCategoryDto>> create(@CurrentUser UUID userId,
                                                              @Valid @RequestBody SubCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(subCategoryService.create(userId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubCategoryDto>> update(@CurrentUser UUID userId,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody SubCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(subCategoryService.update(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        subCategoryService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sub-category deleted"));
    }
}
