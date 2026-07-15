package com.expensetracker.repository;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsBySubCategoryId(UUID subCategoryId);

    /**
     * Paged transaction search. Every filter is optional: a null argument
     * disables that clause. Results are ordered/paginated via {@link Pageable}.
     */
    @Query("""
            select t from Transaction t
            where t.userId = :userId
              and (:categoryId is null or t.categoryId = :categoryId)
              and (:subCategoryId is null or t.subCategoryId = :subCategoryId)
              and (:type is null or t.type = :type)
              and (:from is null or t.transactionDate >= :from)
              and (:to is null or t.transactionDate <= :to)
            """)
    Page<Transaction> search(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("subCategoryId") UUID subCategoryId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
