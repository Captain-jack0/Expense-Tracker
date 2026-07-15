package com.expensetracker.repository;

import com.expensetracker.model.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, UUID> {

    List<SubCategory> findByUserIdOrderByNameAsc(UUID userId);

    List<SubCategory> findByCategoryIdOrderByNameAsc(UUID categoryId);

    Optional<SubCategory> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryIdAndNameIgnoreCase(UUID categoryId, String name);

    long countByCategoryId(UUID categoryId);
}
