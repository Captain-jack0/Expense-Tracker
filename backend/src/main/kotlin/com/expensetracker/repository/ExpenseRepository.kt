package com.expensetracker.repository

import com.expensetracker.model.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ExpenseRepository : JpaRepository<Expense, Long> {

    fun findByCategory(category: String): List<Expense>

    fun findByDateBetween(startDate: LocalDate, endDate: LocalDate): List<Expense>

    @Query("SELECT e FROM Expense e ORDER BY e.date DESC")
    fun findAllOrderByDateDesc(): List<Expense>

    @Query("SELECT DISTINCT e.category FROM Expense e")
    fun findAllCategories(): List<String>
}
