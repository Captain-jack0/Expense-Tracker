# Phase 3 Implementation Guide - Investment & Mobile

**Duration:** Weeks 11-16
**Goal:** Investment Tracking & React Native Mobile App

**Prerequisites:** Phase 2 completed (Transactions and goals working)

---

## Week 11-12: Investment Module

### Day 1-3: Investment Backend

#### Step 1: Create Investment Entity

```kotlin
// backend/src/main/kotlin/com/financetracker/investment/model/Investment.kt
package com.financetracker.investment.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "investments")
data class Investment(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "account_id")
    val accountId: UUID? = null,

    @Column(nullable = false)
    val symbol: String, // AAPL, BTC-USD, etc.

    val name: String? = null,

    val type: String? = null, // stock, etf, crypto, bond

    @Column(nullable = false, precision = 15, scale = 6)
    val quantity: BigDecimal,

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 4)
    val averageCost: BigDecimal,

    @Column(name = "current_price", precision = 15, scale = 4)
    val currentPrice: BigDecimal? = null,

    @Column(name = "last_price_update")
    val lastPriceUpdate: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)

@Entity
@Table(name = "investment_transactions")
data class InvestmentTransaction(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "investment_id", nullable = false)
    val investmentId: UUID,

    @Column(nullable = false)
    val type: String, // buy, sell, dividend

    @Column(nullable = false, precision = 15, scale = 6)
    val quantity: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 4)
    val price: BigDecimal,

    @Column(precision = 10, scale = 2)
    val fees: BigDecimal = BigDecimal.ZERO,

    @Column(name = "transaction_date", nullable = false)
    val transactionDate: java.time.LocalDate,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null
)
```

#### Step 2: Investment Service with Portfolio Calculations

```kotlin
// backend/src/main/kotlin/com/financetracker/investment/service/InvestmentService.kt
package com.financetracker.investment.service

import com.financetracker.investment.dto.*
import com.financetracker.investment.model.Investment
import com.financetracker.investment.repository.InvestmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

@Service
@Transactional
class InvestmentService(
    private val investmentRepository: InvestmentRepository
) {

    fun addInvestment(userId: UUID, request: CreateInvestmentRequest): InvestmentResponse {
        val investment = Investment(
            userId = userId,
            accountId = request.accountId,
            symbol = request.symbol.uppercase(),
            name = request.name,
            type = request.type,
            quantity = request.quantity,
            averageCost = request.price
        )

        val saved = investmentRepository.save(investment)
        return saved.toResponse()
    }

    fun getPortfolio(userId: UUID): PortfolioSummaryResponse {
        val investments = investmentRepository.findByUserId(userId)

        val holdings = investments.map { investment ->
            val currentValue = investment.currentPrice?.let {
                investment.quantity.multiply(it)
            } ?: BigDecimal.ZERO

            val costBasis = investment.quantity.multiply(investment.averageCost)
            val gainLoss = currentValue.subtract(costBasis)
            val gainLossPercent = if (costBasis > BigDecimal.ZERO) {
                gainLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }

            HoldingResponse(
                id = investment.id!!,
                symbol = investment.symbol,
                name = investment.name ?: investment.symbol,
                type = investment.type ?: "unknown",
                quantity = investment.quantity,
                averageCost = investment.averageCost,
                currentPrice = investment.currentPrice,
                currentValue = currentValue,
                costBasis = costBasis,
                gainLoss = gainLoss,
                gainLossPercent = gainLossPercent
            )
        }

        val totalValue = holdings.sumOf { it.currentValue }
        val totalCostBasis = holdings.sumOf { it.costBasis }
        val totalGainLoss = totalValue.subtract(totalCostBasis)
        val totalGainLossPercent = if (totalCostBasis > BigDecimal.ZERO) {
            totalGainLoss.divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        return PortfolioSummaryResponse(
            totalValue = totalValue,
            totalCostBasis = totalCostBasis,
            totalGainLoss = totalGainLoss,
            totalGainLossPercent = totalGainLossPercent,
            holdings = holdings
        )
    }
}
```

### Day 4-7: Investment Frontend

#### Step 3: Portfolio Page with Garden Visualization

```typescript
// frontend/src/pages/investments/InvestmentsPage.tsx
import { useState, useEffect } from 'react';
import { investmentService, PortfolioSummary } from '@/lib/api/investmentService';
import { PortfolioGarden } from '@/components/investments/PortfolioGarden';
import { NumberTicker } from '@/components/animations/NumberTicker';
import { AnimatedPieChart } from '@/components/charts/AnimatedPieChart';

export function InvestmentsPage() {
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPortfolio();
  }, []);

  const loadPortfolio = async () => {
    try {
      const data = await investmentService.getPortfolio();
      setPortfolio(data);
    } catch (error) {
      console.error('Failed to load portfolio', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !portfolio) {
    return <div>Loading...</div>;
  }

  const pieChartData = portfolio.holdings.map(holding => ({
    name: holding.symbol,
    value: holding.currentValue,
    color: getColorForSymbol(holding.symbol)
  }));

  return (
    <div className="investments-page">
      <h1>Investment Portfolio</h1>

      <div className="portfolio-summary">
        <div className="summary-card">
          <h3>Total Value</h3>
          <NumberTicker
            value={portfolio.totalValue}
            prefix="$"
            decimals={2}
            duration={1.5}
          />
        </div>

        <div className="summary-card">
          <h3>Total Gain/Loss</h3>
          <div className={portfolio.totalGainLoss >= 0 ? 'gain' : 'loss'}>
            <NumberTicker
              value={portfolio.totalGainLoss}
              prefix="$"
              decimals={2}
            />
            <span className="percent">
              ({portfolio.totalGainLossPercent.toFixed(2)}%)
            </span>
          </div>
        </div>
      </div>

      <PortfolioGarden holdings={portfolio.holdings} />

      <div className="charts-row">
        <AnimatedPieChart
          data={pieChartData}
          title="Portfolio Allocation"
        />
      </div>

      <div className="holdings-table">
        <table>
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Quantity</th>
              <th>Avg Cost</th>
              <th>Current Price</th>
              <th>Value</th>
              <th>Gain/Loss</th>
            </tr>
          </thead>
          <tbody>
            {portfolio.holdings.map(holding => (
              <tr key={holding.id}>
                <td>{holding.symbol}</td>
                <td>{holding.quantity.toFixed(4)}</td>
                <td>${holding.averageCost.toFixed(2)}</td>
                <td>${holding.currentPrice?.toFixed(2) || 'N/A'}</td>
                <td>${holding.currentValue.toFixed(2)}</td>
                <td className={holding.gainLoss >= 0 ? 'gain' : 'loss'}>
                  ${holding.gainLoss.toFixed(2)}
                  ({holding.gainLossPercent.toFixed(2)}%)
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function getColorForSymbol(symbol: string): string {
  const colors = [
    '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'
  ];
  const hash = symbol.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return colors[hash % colors.length];
}
```

---

## Week 13-14: Mobile App Setup

### Day 8-9: React Native Project Setup

#### Step 4: Initialize Mobile Project

```bash
cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker"
npx create-expo-app mobile --template blank-typescript

cd mobile

# Install dependencies
npm install @react-navigation/native @react-navigation/bottom-tabs
npm install @react-navigation/native-stack
npm install react-native-screens react-native-safe-area-context
npm install react-native-reanimated react-native-gesture-handler
npm install @tanstack/react-query axios
npm install expo-secure-store
npm install react-native-svg
npm install zustand
```

#### Step 5: Configure Navigation

```typescript
// mobile/app/_layout.tsx
import { Stack } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';

const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(auth)" options={{ headerShown: false }} />
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        </Stack>
      </AuthProvider>
    </QueryClientProvider>
  );
}
```

#### Step 6: Create Mobile Auth Screens

```typescript
// mobile/app/(auth)/login.tsx
import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '@/contexts/AuthContext';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const router = useRouter();

  const handleLogin = async () => {
    try {
      await login({ email, password });
      router.replace('/(tabs)');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome Back</Text>

      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />

      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <TouchableOpacity style={styles.button} onPress={handleLogin}>
        <Text style={styles.buttonText}>Log In</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => router.push('/(auth)/register')}>
        <Text style={styles.link}>Don't have an account? Sign up</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#fff'
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    marginBottom: 40,
    textAlign: 'center'
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 15,
    marginBottom: 15,
    fontSize: 16
  },
  button: {
    backgroundColor: '#2563eb',
    padding: 15,
    borderRadius: 8,
    marginTop: 10
  },
  buttonText: {
    color: '#fff',
    textAlign: 'center',
    fontSize: 16,
    fontWeight: 'bold'
  },
  error: {
    color: '#ef4444',
    marginBottom: 10
  },
  link: {
    color: '#2563eb',
    textAlign: 'center',
    marginTop: 20
  }
});
```

### Day 10-11: Create Shared Package

#### Step 7: Set Up Shared Package

```bash
cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker"
mkdir shared
cd shared
npm init -y
```

```json
// shared/package.json
{
  "name": "@finance-tracker/shared",
  "version": "1.0.0",
  "main": "src/index.ts",
  "types": "src/index.ts",
  "scripts": {
    "build": "tsc",
    "dev": "tsc --watch"
  },
  "dependencies": {
    "axios": "^1.6.0",
    "zod": "^3.22.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "@types/node": "^20.0.0"
  }
}
```

#### Step 8: Move Shared Code

```typescript
// shared/src/types/transaction.ts
export interface Transaction {
  id: string;
  userId: string;
  accountId: string;
  categoryId?: string;
  type: 'income' | 'expense' | 'transfer';
  amount: number;
  currency: string;
  description?: string;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: string;
  name: string;
  type: 'checking' | 'savings' | 'credit' | 'investment' | 'cash';
  currency: string;
  initialBalance: number;
  currentBalance: number;
  isActive: boolean;
}

export interface Goal {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate?: string;
  icon?: string;
  color?: string;
  status: 'active' | 'completed' | 'cancelled';
}
```

```typescript
// shared/src/validation/schemas.ts
import { z } from 'zod';

export const transactionSchema = z.object({
  accountId: z.string().uuid(),
  amount: z.number().positive().max(9999999999.99),
  type: z.enum(['income', 'expense', 'transfer']),
  description: z.string().max(500).optional(),
  transactionDate: z.string()
});

export type TransactionInput = z.infer<typeof transactionSchema>;
```

#### Step 9: Use Shared Package in Both Apps

```json
// frontend/package.json
{
  "dependencies": {
    "@finance-tracker/shared": "file:../shared"
  }
}

// mobile/package.json
{
  "dependencies": {
    "@finance-tracker/shared": "file:../shared"
  }
}
```

```bash
# Install in both
cd frontend && npm install
cd ../mobile && npm install
```

### Day 12-14: Mobile Core Screens

#### Step 10: Dashboard Screen with Animations

```typescript
// mobile/app/(tabs)/index.tsx
import { useState, useEffect } from 'react';
import { View, Text, ScrollView, StyleSheet } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { accountService } from '@finance-tracker/shared';
import { ProgressRing } from '@/components/animations/ProgressRing';

export default function DashboardScreen() {
  const [summary, setSummary] = useState<any>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const data = await accountService.getSummary();
    setSummary(data);
  };

  if (!summary) {
    return <View style={styles.loading}><Text>Loading...</Text></View>;
  }

  return (
    <ScrollView style={styles.container}>
      <Animated.View entering={FadeInDown.delay(100)} style={styles.card}>
        <Text style={styles.cardTitle}>Total Balance</Text>
        <Text style={styles.amount}>${summary.totalBalance.toFixed(2)}</Text>
      </Animated.View>

      <Animated.View entering={FadeInDown.delay(200)} style={styles.card}>
        <Text style={styles.cardTitle}>Accounts</Text>
        {summary.accounts.map((account: any) => (
          <View key={account.id} style={styles.accountRow}>
            <Text>{account.name}</Text>
            <Text>${account.currentBalance.toFixed(2)}</Text>
          </View>
        ))}
      </Animated.View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb'
  },
  loading: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center'
  },
  card: {
    backgroundColor: '#fff',
    margin: 15,
    padding: 20,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10
  },
  amount: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#2563eb'
  },
  accountRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0'
  }
});
```

---

## Week 15-16: Testing & Polish

### Day 15-21: Complete Mobile Features & Testing

- Implement all CRUD operations in mobile
- Add biometric authentication
- Test on iOS and Android
- Performance optimization
- E2E testing with Detox

---

**Mobile app complete and ready for Phase 4!**
