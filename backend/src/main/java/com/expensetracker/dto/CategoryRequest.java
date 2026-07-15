package com.expensetracker.dto;

import com.expensetracker.model.Bucket;
import com.expensetracker.model.CategoryKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Kind is required (INCOME or EXPENSE)")
    private CategoryKind kind;

    @NotNull(message = "Bucket is required (INCOME, NEEDS, WANTS, SAVINGS, OTHER)")
    private Bucket bucket;

    private String icon;

    private String color;
}
