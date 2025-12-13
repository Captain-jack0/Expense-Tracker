package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByCategory(String category);

    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Expense e ORDER BY e.date DESC")
    List<Expense> findAllOrderByDateDesc();

    @Query("SELECT DISTINCT e.category FROM Expense e")
    List<String> findAllCategories();
}
