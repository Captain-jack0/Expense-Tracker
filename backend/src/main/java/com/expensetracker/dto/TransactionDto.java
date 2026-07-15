package com.expensetracker.dto;

import com.expensetracker.model.SourceType;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private String id;
    private String name;
    private BigDecimal amount;
    private String currency;
    private String categoryId;
    private String subCategoryId;
    private TransactionType type;
    private String transactionDate;
    private String note;
    private SourceType sourceType;
    private Integer installmentNo;
    private String createdAt;
    private String updatedAt;

    public static TransactionDto from(Transaction t) {
        return new TransactionDto(
                t.getId().toString(),
                t.getName(),
                t.getAmount(),
                t.getCurrency(),
                t.getCategoryId().toString(),
                t.getSubCategoryId() != null ? t.getSubCategoryId().toString() : null,
                t.getType(),
                t.getTransactionDate().toString(),
                t.getNote(),
                t.getSourceType(),
                t.getInstallmentNo(),
                t.getCreatedAt().toString(),
                t.getUpdatedAt().toString()
        );
    }
}
