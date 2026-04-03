package com.expensetracker.controller

import com.expensetracker.model.Expense
import com.expensetracker.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = ["*"])
class ExpenseController(
    private val expenseService: ExpenseService
) {

    @GetMapping
    fun getAllExpenses(): ResponseEntity<List<Expense>> {
        return ResponseEntity.ok(expenseService.getAllExpenses())
    }

    @GetMapping("/{id}")
    fun getExpenseById(@PathVariable id: Long): ResponseEntity<Expense> {
        return expenseService.getExpenseById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    fun createExpense(@Valid @RequestBody expense: Expense): ResponseEntity<Expense> {
        val createdExpense = expenseService.createExpense(expense)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdExpense)
    }

    @PutMapping("/{id}")
    fun updateExpense(
        @PathVariable id: Long,
        @Valid @RequestBody expense: Expense
    ): ResponseEntity<Expense> {
        return try {
            val updatedExpense = expenseService.updateExpense(id, expense)
            ResponseEntity.ok(updatedExpense)
        } catch (e: RuntimeException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteExpense(@PathVariable id: Long): ResponseEntity<Void> {
        expenseService.deleteExpense(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/category/{category}")
    fun getExpensesByCategory(@PathVariable category: String): ResponseEntity<List<Expense>> {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(category))
    }

    @GetMapping("/date-range")
    fun getExpensesByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<Expense>> {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(startDate, endDate))
    }

    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(expenseService.getAllCategories())
    }
}
