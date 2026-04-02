package com.expensetracker.service

import com.expensetracker.model.Expense
import com.expensetracker.repository.ExpenseRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {

    fun getAllExpenses(): List<Expense> {
        return expenseRepository.findAllOrderByDateDesc()
    }

    fun getExpenseById(id: Long): Expense? {
        return expenseRepository.findById(id).orElse(null)
    }

    fun createExpense(expense: Expense): Expense {
        return expenseRepository.save(expense)
    }

    fun updateExpense(id: Long, expenseDetails: Expense): Expense {
        val expense = expenseRepository.findById(id)
            .orElseThrow { RuntimeException("Expense not found with id: $id") }

        expense.title = expenseDetails.title
        expense.amount = expenseDetails.amount
        expense.category = expenseDetails.category
        expense.description = expenseDetails.description
        expense.date = expenseDetails.date

        return expenseRepository.save(expense)
    }

    fun deleteExpense(id: Long) {
        expenseRepository.deleteById(id)
    }

    fun getExpensesByCategory(category: String): List<Expense> {
        return expenseRepository.findByCategory(category)
    }

    fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): List<Expense> {
        return expenseRepository.findByDateBetween(startDate, endDate)
    }

    fun getAllCategories(): List<String> {
        return expenseRepository.findAllCategories()
    }
}
