package com.expensetracker.dto;

import com.expensetracker.model.SubCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryDto {

    private String id;
    private String categoryId;
    private String name;
    private String icon;
    private String color;
    private BigDecimal monthlyLimit;

    @JsonProperty("isSystem")
    private boolean isSystem;

    public static SubCategoryDto from(SubCategory s) {
        return new SubCategoryDto(
                s.getId().toString(),
                s.getCategoryId().toString(),
                s.getName(),
                s.getIcon(),
                s.getColor(),
                s.getMonthlyLimit(),
                s.isSystem()
        );
    }
}
