package com.expensetracker.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "expenses")
data class Expense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @field:NotBlank(message = "Title is required")
    var title: String = "",

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank(message = "Category is required")
    var category: String = "",

    var description: String? = null,

    @field:NotNull(message = "Date is required")
    var date: LocalDate = LocalDate.now(),

    @Column(name = "created_at")
    var createdAt: LocalDate? = null
) {
    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDate.now()
    }
}
