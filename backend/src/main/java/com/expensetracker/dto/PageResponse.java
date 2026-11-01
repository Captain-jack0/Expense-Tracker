package com.expensetracker.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response shape consumed by the frontend's
 * {@code PaginatedResponse<T>}:
 * {@code { success, data: T[], pagination: { total, page, limit, totalPages } }}.
 * {@code page} is 1-based.
 */
public record PageResponse<T>(boolean success, List<T> data, Pagination pagination) {

    public record Pagination(long total, int page, int limit, int totalPages) {}

    public static <T> PageResponse<T> of(Page<T> page, int oneBasedPage, int limit) {
        return new PageResponse<>(true, page.getContent(),
                new Pagination(page.getTotalElements(), oneBasedPage, limit, page.getTotalPages()));
    }
}
