package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SubCategoryRequest {

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    private String icon;

    private String color;

    /** Optional monthly spending cap. When present it must be greater than 0. */
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly limit must be greater than 0")
    private BigDecimal monthlyLimit;
}
