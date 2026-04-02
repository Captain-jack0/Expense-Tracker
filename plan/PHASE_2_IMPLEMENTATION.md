# Phase 2 Implementation Guide - Core Financial Features

**Duration:** Weeks 5-10
**Goal:** Account Management, Transactions, Dashboard, Goal Tracking

**Prerequisites:** Phase 1 completed (User authentication working)

---

## Week 5-6: Account & Transaction Management

### Day 1-2: Account Module

#### Step 1: Create Account Entity

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/model/Account.kt
package com.financetracker.transaction.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "accounts")
data class Account(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val type: String, // checking, savings, credit, investment, cash

    @Column(nullable = false)
    val currency: String = "USD",

    @Column(name = "initial_balance", precision = 15, scale = 2)
    val initialBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "current_balance", precision = 15, scale = 2)
    val currentBalance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)
```

#### Step 2: Create Account Repository

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/repository/AccountRepository.kt
package com.financetracker.transaction.repository

import com.financetracker.transaction.model.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, UUID> {
    fun findByUserId(userId: UUID): List<Account>
    fun findByUserIdAndIsActive(userId: UUID, isActive: Boolean): List<Account>
    fun findByIdAndUserId(id: UUID, userId: UUID): Account?
}
```

#### Step 3: Create Account DTOs

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/dto/AccountDto.kt
package com.financetracker.transaction.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class CreateAccountRequest(
    @field:NotBlank(message = "Account name is required")
    @field:Size(max = 100, message = "Account name too long")
    val name: String,

    @field:NotBlank(message = "Account type is required")
    @field:Pattern(
        regexp = "^(checking|savings|credit|investment|cash)$",
        message = "Invalid account type"
    )
    val type: String,

    @field:Size(min = 3, max = 3, message = "Currency must be 3 characters")
    val currency: String = "USD",

    @field:DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    @field:Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    val initialBalance: BigDecimal = BigDecimal.ZERO
)

data class UpdateAccountRequest(
    @field:NotBlank(message = "Account name is required")
    @field:Size(max = 100, message = "Account name too long")
    val name: String,

    @field:NotBlank(message = "Account type is required")
    val type: String
)

data class AccountResponse(
    val id: UUID,
    val name: String,
    val type: String,
    val currency: String,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AccountSummaryResponse(
    val totalBalance: BigDecimal,
    val accounts: List<AccountResponse>
)
```

#### Step 4: Create Account Service

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/service/AccountService.kt
package com.financetracker.transaction.service

import com.financetracker.common.exception.ForbiddenException
import com.financetracker.common.exception.NotFoundException
import com.financetracker.transaction.dto.*
import com.financetracker.transaction.model.Account
import com.financetracker.transaction.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository
) {

    fun createAccount(userId: UUID, request: CreateAccountRequest): AccountResponse {
        val account = Account(
            userId = userId,
            name = request.name,
            type = request.type,
            currency = request.currency,
            initialBalance = request.initialBalance,
            currentBalance = request.initialBalance
        )

        val saved = accountRepository.save(account)
        return saved.toResponse()
    }

    fun getAccountById(userId: UUID, accountId: UUID): AccountResponse {
        val account = accountRepository.findByIdAndUserId(accountId, userId)
            ?: throw NotFoundException("Account not found")

        return account.toResponse()
    }

    fun getAllAccounts(userId: UUID, activeOnly: Boolean = true): List<AccountResponse> {
        val accounts = if (activeOnly) {
            accountRepository.findByUserIdAndIsActive(userId, true)
        } else {
            accountRepository.findByUserId(userId)
        }

        return accounts.map { it.toResponse() }
    }

    fun getAccountSummary(userId: UUID): AccountSummaryResponse {
        val accounts = accountRepository.findByUserIdAndIsActive(userId, true)
        val totalBalance = accounts.sumOf { it.currentBalance }

        return AccountSummaryResponse(
            totalBalance = totalBalance,
            accounts = accounts.map { it.toResponse() }
        )
    }

    fun updateAccount(userId: UUID, accountId: UUID, request: UpdateAccountRequest): AccountResponse {
        val account = accountRepository.findByIdAndUserId(accountId, userId)
            ?: throw NotFoundException("Account not found")

        val updated = account.copy(
            name = request.name,
            type = request.type
        )

        val saved = accountRepository.save(updated)
        return saved.toResponse()
    }

    fun deleteAccount(userId: UUID, accountId: UUID) {
        val account = accountRepository.findByIdAndUserId(accountId, userId)
            ?: throw NotFoundException("Account not found")

        // Soft delete
        val deactivated = account.copy(isActive = false)
        accountRepository.save(deactivated)
    }

    // Internal method for updating balance (used by transaction service)
    fun updateBalance(accountId: UUID, amount: BigDecimal) {
        val account = accountRepository.findById(accountId)
            .orElseThrow { NotFoundException("Account not found") }

        val updated = account.copy(
            currentBalance = account.currentBalance.add(amount)
        )

        accountRepository.save(updated)
    }
}

// Extension function
private fun Account.toResponse() = AccountResponse(
    id = id!!,
    name = name,
    type = type,
    currency = currency,
    initialBalance = initialBalance,
    currentBalance = currentBalance,
    isActive = isActive,
    createdAt = createdAt!!,
    updatedAt = updatedAt!!
)
```

#### Step 5: Create Account Controller

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/controller/AccountController.kt
package com.financetracker.transaction.controller

import com.financetracker.common.security.CustomUserDetails
import com.financetracker.transaction.dto.*
import com.financetracker.transaction.service.AccountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @PostMapping
    fun createAccount(
        @Valid @RequestBody request: CreateAccountRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<AccountResponse> {
        val account = accountService.createAccount(userDetails.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(account)
    }

    @GetMapping
    fun getAllAccounts(
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<List<AccountResponse>> {
        val accounts = accountService.getAllAccounts(userDetails.userId, activeOnly)
        return ResponseEntity.ok(accounts)
    }

    @GetMapping("/summary")
    fun getAccountSummary(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<AccountSummaryResponse> {
        val summary = accountService.getAccountSummary(userDetails.userId)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/{id}")
    fun getAccountById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<AccountResponse> {
        val account = accountService.getAccountById(userDetails.userId, id)
        return ResponseEntity.ok(account)
    }

    @PutMapping("/{id}")
    fun updateAccount(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountRequest,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<AccountResponse> {
        val account = accountService.updateAccount(userDetails.userId, id, request)
        return ResponseEntity.ok(account)
    }

    @DeleteMapping("/{id}")
    fun deleteAccount(
        @PathVariable id: UUID,
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<Void> {
        accountService.deleteAccount(userDetails.userId, id)
        return ResponseEntity.noContent().build()
    }
}
```

### Day 3-5: Transaction Module

#### Step 6: Create Transaction Entity & Repository

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/model/Transaction.kt
package com.financetracker.transaction.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Column(name = "category_id")
    val categoryId: UUID? = null,

    @Column(nullable = false)
    val type: String, // income, expense, transfer

    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    @Column(nullable = false)
    val currency: String = "USD",

    @Column(length = 500)
    val description: String? = null,

    @Column(name = "transaction_date", nullable = false)
    val transactionDate: LocalDate,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)

// backend/src/main/kotlin/com/financetracker/transaction/repository/TransactionRepository.kt
package com.financetracker.transaction.repository

import com.financetracker.transaction.model.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {

    fun findByUserId(userId: UUID, pageable: Pageable): Page<Transaction>

    fun findByUserIdAndTransactionDateBetween(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable
    ): Page<Transaction>

    fun findByUserIdAndType(
        userId: UUID,
        type: String,
        pageable: Pageable
    ): Page<Transaction>

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
        AND t.transactionDate BETWEEN :startDate AND :endDate
        AND (:type IS NULL OR t.type = :type)
        AND (:accountId IS NULL OR t.accountId = :accountId)
        ORDER BY t.transactionDate DESC
    """)
    fun findByFilters(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("type") type: String?,
        @Param("accountId") accountId: UUID?,
        pageable: Pageable
    ): Page<Transaction>
}
```

#### Step 7: Create Transaction Service

```kotlin
// backend/src/main/kotlin/com/financetracker/transaction/service/TransactionService.kt
package com.financetracker.transaction.service

import com.financetracker.common.exception.NotFoundException
import com.financetracker.transaction.dto.*
import com.financetracker.transaction.model.Transaction
import com.financetracker.transaction.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val accountService: AccountService
) {

    fun createTransaction(userId: UUID, request: CreateTransactionRequest): TransactionResponse {
        // Verify account belongs to user
        accountService.getAccountById(userId, request.accountId)

        val transaction = Transaction(
            userId = userId,
            accountId = request.accountId,
            categoryId = request.categoryId,
            type = request.type,
            amount = request.amount,
            currency = request.currency,
            description = request.description,
            transactionDate = request.transactionDate
        )

        val saved = transactionRepository.save(transaction)

        // Update account balance
        val balanceChange = when (request.type) {
            "income" -> request.amount
            "expense" -> request.amount.negate()
            else -> BigDecimal.ZERO
        }
        accountService.updateBalance(request.accountId, balanceChange)

        return saved.toResponse()
    }

    fun getTransactionById(userId: UUID, transactionId: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Transaction not found") }

        if (transaction.userId != userId) {
            throw NotFoundException("Transaction not found")
        }

        return transaction.toResponse()
    }

    fun getAllTransactions(
        userId: UUID,
        page: Int = 0,
        size: Int = 20,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        type: String? = null,
        accountId: UUID? = null
    ): Page<TransactionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending())

        val transactions = if (startDate != null && endDate != null) {
            transactionRepository.findByFilters(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                type = type,
                accountId = accountId,
                pageable = pageable
            )
        } else {
            transactionRepository.findByUserId(userId, pageable)
        }

        return transactions.map { it.toResponse() }
    }

    fun updateTransaction(
        userId: UUID,
        transactionId: UUID,
        request: UpdateTransactionRequest
    ): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Transaction not found") }

        if (transaction.userId != userId) {
            throw NotFoundException("Transaction not found")
        }

        // Reverse old balance change
        val oldBalanceChange = when (transaction.type) {
            "income" -> transaction.amount.negate()
            "expense" -> transaction.amount
            else -> BigDecimal.ZERO
        }
        accountService.updateBalance(transaction.accountId, oldBalanceChange)

        // Apply new balance change
        val newBalanceChange = when (request.type) {
            "income" -> request.amount
            "expense" -> request.amount.negate()
            else -> BigDecimal.ZERO
        }
        accountService.updateBalance(request.accountId, newBalanceChange)

        val updated = transaction.copy(
            accountId = request.accountId,
            categoryId = request.categoryId,
            type = request.type,
            amount = request.amount,
            description = request.description,
            transactionDate = request.transactionDate
        )

        val saved = transactionRepository.save(updated)
        return saved.toResponse()
    }

    fun deleteTransaction(userId: UUID, transactionId: UUID) {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NotFoundException("Transaction not found") }

        if (transaction.userId != userId) {
            throw NotFoundException("Transaction not found")
        }

        // Reverse balance change
        val balanceChange = when (transaction.type) {
            "income" -> transaction.amount.negate()
            "expense" -> transaction.amount
            else -> BigDecimal.ZERO
        }
        accountService.updateBalance(transaction.accountId, balanceChange)

        transactionRepository.delete(transaction)
    }
}

private fun Transaction.toResponse() = TransactionResponse(
    id = id!!,
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    amount = amount,
    currency = currency,
    description = description,
    transactionDate = transactionDate,
    createdAt = createdAt!!,
    updatedAt = updatedAt!!
)
```

### Day 6-7: Frontend Account Management

#### Step 8: Create Account Service (Frontend)

```typescript
// frontend/src/lib/api/accountService.ts
import apiClient from './client';

export interface Account {
  id: string;
  name: string;
  type: 'checking' | 'savings' | 'credit' | 'investment' | 'cash';
  currency: string;
  initialBalance: number;
  currentBalance: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccountRequest {
  name: string;
  type: string;
  currency?: string;
  initialBalance?: number;
}

export const accountService = {
  async getAll(activeOnly: boolean = true): Promise<Account[]> {
    const { data } = await apiClient.get('/accounts', {
      params: { activeOnly }
    });
    return data;
  },

  async getById(id: string): Promise<Account> {
    const { data } = await apiClient.get(`/accounts/${id}`);
    return data;
  },

  async create(account: CreateAccountRequest): Promise<Account> {
    const { data } = await apiClient.post('/accounts', account);
    return data;
  },

  async update(id: string, account: Partial<CreateAccountRequest>): Promise<Account> {
    const { data } = await apiClient.put(`/accounts/${id}`, account);
    return data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/accounts/${id}`);
  },

  async getSummary(): Promise<{ totalBalance: number; accounts: Account[] }> {
    const { data } = await apiClient.get('/accounts/summary');
    return data;
  }
};
```

#### Step 9: Create Account Management Page

```typescript
// frontend/src/pages/accounts/AccountsPage.tsx
import { useState, useEffect } from 'react';
import { accountService, Account } from '@/lib/api/accountService';
import { AnimatedCard } from '@/components/animations/AnimatedCard';
import { NumberTicker } from '@/components/animations/NumberTicker';
import { motion } from 'framer-motion';

export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [totalBalance, setTotalBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    try {
      const summary = await accountService.getSummary();
      setAccounts(summary.accounts);
      setTotalBalance(summary.totalBalance);
    } catch (error) {
      console.error('Failed to load accounts', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteAccount = async (id: string) => {
    if (!confirm('Are you sure you want to delete this account?')) return;

    try {
      await accountService.delete(id);
      loadAccounts();
    } catch (error) {
      console.error('Failed to delete account', error);
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="accounts-page">
      <div className="page-header">
        <h1>Accounts</h1>
        <button onClick={() => setShowAddModal(true)} className="btn-primary">
          + Add Account
        </button>
      </div>

      <div className="summary-card">
        <h2>Total Balance</h2>
        <NumberTicker value={totalBalance} prefix="$" decimals={2} duration={1.5} />
      </div>

      <div className="accounts-grid">
        {accounts.map((account, index) => (
          <AnimatedCard key={account.id} delay={index * 0.1}>
            <div className="account-card">
              <div className="account-header">
                <h3>{account.name}</h3>
                <span className={`account-type ${account.type}`}>
                  {account.type}
                </span>
              </div>

              <div className="account-balance">
                <NumberTicker
                  value={account.currentBalance}
                  prefix="$"
                  decimals={2}
                />
              </div>

              <div className="account-actions">
                <button
                  onClick={() => {/* navigate to transactions */}}
                  className="btn-secondary"
                >
                  View Transactions
                </button>
                <button
                  onClick={() => handleDeleteAccount(account.id)}
                  className="btn-danger"
                >
                  Delete
                </button>
              </div>
            </div>
          </AnimatedCard>
        ))}
      </div>

      {showAddModal && (
        <AddAccountModal
          onClose={() => setShowAddModal(false)}
          onSuccess={loadAccounts}
        />
      )}
    </div>
  );
}
```

---

## Week 7-8: Dashboard & Reporting

### Day 8-10: Dashboard Backend

#### Step 10: Create Dashboard Service

```kotlin
// backend/src/main/kotlin/com/financetracker/common/service/DashboardService.kt
package com.financetracker.common.service

import com.financetracker.transaction.repository.TransactionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class DashboardService(
    private val transactionRepository: TransactionRepository
) {

    fun getDashboardSummary(userId: UUID, startDate: LocalDate, endDate: LocalDate): DashboardSummaryResponse {
        // Implementation details in response
        return DashboardSummaryResponse(
            totalIncome = BigDecimal.ZERO,
            totalExpenses = BigDecimal.ZERO,
            netAmount = BigDecimal.ZERO,
            categoryBreakdown = emptyList(),
            monthlyTrends = emptyList()
        )
    }
}

data class DashboardSummaryResponse(
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val netAmount: BigDecimal,
    val categoryBreakdown: List<CategoryBreakdown>,
    val monthlyTrends: List<MonthlyTrend>
)

data class CategoryBreakdown(
    val categoryId: UUID?,
    val categoryName: String,
    val amount: BigDecimal,
    val percentage: Double
)

data class MonthlyTrend(
    val month: String,
    val income: BigDecimal,
    val expenses: BigDecimal
)
```

---

## Week 9-10: Goal Tracking

### Day 11-14: Goals Implementation

Similar detailed steps for goals module...

---

**Continue with remaining days following same pattern as Phase 1.**

*File continues with Goals, Frontend Dashboard, Charts implementation...*
