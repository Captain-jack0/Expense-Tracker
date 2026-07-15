package com.expensetracker.dto;

import com.expensetracker.model.Bucket;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryKind;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private String id;
    private String name;
    private CategoryKind kind;
    private Bucket bucket;
    private String icon;
    private String color;

    @JsonProperty("isSystem")
    private boolean isSystem;

    private int sortOrder;

    public static CategoryDto from(Category c) {
        return new CategoryDto(
                c.getId().toString(),
                c.getName(),
                c.getKind(),
                c.getBucket(),
                c.getIcon(),
                c.getColor(),
                c.isSystem(),
                c.getSortOrder()
        );
    }
}
