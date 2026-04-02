# Personal Finance Tracker & Investment Assistant - Implementation Plan

## Project Overview

Build a gamified personal finance application (web + mobile) that goes beyond basic expense tracking to provide:
- Expense and income management
- Investment tracking with visual progress
- Goal-based savings tracking
- AI-powered financial coaching and recommendations

## Tech Stack Decision

### Backend: Spring Boot + Kotlin 📅
- Enterprise-grade, industry standard
- Null safety and modern syntax
- Robust ecosystem for financial applications
- High job market value

### Frontend: React (Web) + React Native (Mobile) 📅
- Modern, component-based architecture
- 60-70% code reuse between web and mobile
- Excellent animation libraries (Framer Motion, Lottie)

### Database: PostgreSQL 📅
- ACID compliance for financial data
- JSON support for flexibility
- Rich data types (DECIMAL for money)
- Battle-tested in financial systems

### Cache: Redis 📅
- Start with self-hosted Docker Redis
- Migrate to Upstash for serverless production

### DevOps (MVP): Simplified Stack 📅
- Docker Compose (local dev)
- GitHub Actions (CI/CD)
- Vercel (web frontend deployment)
- Railway or Render (backend + PostgreSQL)

### DevOps (Post-MVP): Full Stack
- Kubernetes + Rancher
- Jenkins
- Comprehensive monitoring

## Architecture: Modular Monolith → Microservices

### Phase 1-3: Modular Monolith
- Single deployable unit
- Clear module boundaries (user, transaction, goal, investment, coach)
- Easier debugging and deployment
- Lower infrastructure cost

### Phase 4+: Extract to Microservices (if needed)
- AI Coach service
- Investment data service
- Notification service

## Development Approach: Web First

### Rationale:
1. Faster iteration cycles
2. Easier debugging and testing
3. React Native shares 60-70% of code with React web
4. Web as reference implementation for mobile
5. Users can access via mobile browsers while native app is developed

### Timeline:
- **Phase 1-2**: Web application (Weeks 1-10)
- **Phase 3**: React Native mobile app (Weeks 11-16)
- **Phase 4**: AI Coach & Advanced features (Weeks 17-24)

## MVP vs Future Features

### MVP (Phases 1-2) - Weeks 1-10
📅 **Core Features (Planned):**
- Single user accounts
- Basic expense/income tracking
- Manual categorization
- Simple goal tracker with progress visualization
- Basic investment tracking (manual entry)
- Dashboard with account overview
- Basic animations and visual feedback

### Post-MVP - Weeks 11+
🔮 **Future Features:**
- Household/shared accounts
- AI financial coach
- Bank account integration (Plaid/Yodlee)
- Advanced gamification (achievements, streaks, leaderboards)
- Investment API integrations
- Advanced analytics and forecasting

## Database Schema

### Core Tables (Phase 1-2)

```sql
-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(50) DEFAULT 'UTC',
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Accounts (bank accounts, cash, etc.)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- checking, savings, credit, investment, cash
    currency VARCHAR(3) DEFAULT 'USD',
    initial_balance DECIMAL(15, 2) DEFAULT 0,
    current_balance DECIMAL(15, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- income, expense, transfer
    icon VARCHAR(50),
    color VARCHAR(7),
    parent_id UUID REFERENCES categories(id),
    is_system BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Transactions
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id),
    type VARCHAR(20) NOT NULL, -- income, expense, transfer
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description VARCHAR(500),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Goals
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(15, 2) NOT NULL,
    current_amount DECIMAL(15, 2) DEFAULT 0,
    target_date DATE,
    icon VARCHAR(50),
    color VARCHAR(7),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Investments
CREATE TABLE investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID REFERENCES accounts(id),
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    type VARCHAR(50), -- stock, etf, mutual_fund, crypto, bond
    quantity DECIMAL(15, 6) NOT NULL,
    average_cost DECIMAL(15, 4) NOT NULL,
    current_price DECIMAL(15, 4),
    last_price_update TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Critical indexes
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_investments_user_id ON investments(user_id);
```

## Implementation Phases

### Phase 1: Foundation & Core Infrastructure (Weeks 1-4)

#### Key Deliverables (Planned):
- 📅 Spring Boot + Kotlin project setup
- 📅 PostgreSQL + Redis via Docker Compose
- 📅 Security configuration (JWT, encryption)
- 📅 User registration and authentication
- 📅 React frontend with Vite + TypeScript
- 📅 Protected routes and auth context

#### Critical Files:
1. `backend/pom.xml` - Dependencies
2. `backend/src/main/resources/application.yml` - Configuration
3. `backend/src/main/java/com/financetracker/common/security/SecurityConfig.kt` - Spring Security
4. `backend/src/main/java/com/financetracker/common/security/JwtTokenProvider.kt` - JWT
5. `backend/src/main/java/com/financetracker/user/service/UserService.kt` - User management
6. `frontend/src/contexts/AuthContext.tsx` - Auth state
7. `docker-compose.yml` - Local environment

### Phase 2: Core Financial Features (Weeks 5-10)

#### Key Deliverables (Planned):
- 📅 Account management (CRUD)
- 📅 Transaction tracking (income/expense/investment)
- 📅 Category system
- 📅 Dashboard with animated charts
- 📅 Goal tracker with progress visualization
- 📅 Basic investment tracking

#### Critical Files:
1. `backend/src/main/java/com/financetracker/transaction/service/TransactionService.kt`
2. `backend/src/main/java/com/financetracker/goal/service/GoalService.kt`
3. `frontend/src/pages/dashboard/DashboardPage.tsx`
4. `frontend/src/components/animations/ProgressRing.tsx`
5. `frontend/src/components/charts/CategoryPieChart.tsx`

### Phase 3: Investment Tracking & Mobile (Weeks 11-16)

#### Key Deliverables (Planned):
- 📅 Investment portfolio tracking
- 📅 Buy/sell transactions
- 📅 Performance calculations
- 📅 React Native mobile app
- 📅 Shared code between web and mobile
- 📅 Mobile authentication and core screens

### Phase 4: AI Coach & Advanced Features (Weeks 17-24)

#### Key Deliverables (Planned):
- 📅 AI financial coach (OpenAI integration)
- 📅 Spending insights and recommendations
- 📅 Household accounts
- 📅 Achievement system
- 📅 Advanced gamification

## Security Requirements

### Critical for Financial Data:
1. **Authentication**: JWT with refresh tokens, OAuth 2.0
2. **Password Storage**: bcrypt with cost factor 12+
3. **Data Encryption**: TLS 1.3 in transit, AES-256 at rest
4. **API Security**: Rate limiting, input validation, CORS
5. **Session Management**: Secure cookies, session timeout
6. **Database**: Row-level security, parameterized queries
7. **Audit Logging**: All financial operations logged
8. **2FA**: TOTP-based (Phase 2+)
9. **PII Handling**: Encrypt SSN, account numbers

## Animation & Gamification

### Libraries:
- **Framer Motion**: Micro-interactions and transitions
- **Lottie**: Complex animations from designers
- **Recharts**: Animated charts
- **canvas-confetti**: Goal completion celebrations
- **React Native Reanimated**: 60fps mobile animations

### Gamification Elements:
1. **Progress Visualization**: Animated progress bars, growing plant metaphor
2. **Achievements**: Streak tracking, badges for milestones
3. **Visual Feedback**: Color-coded spending, celebration animations
4. **Sound Effects**: Positive reinforcement for actions

## Testing Strategy

### Unit Tests:
- JUnit 5 + Mockito (backend)
- Vitest + React Testing Library (frontend)
- Target: 80%+ coverage

### Integration Tests:
- TestContainers for database tests
- MSW for API integration tests

### E2E Tests:
- Playwright for critical user flows
- Test: Registration, transaction CRUD, goal tracking, dashboard

## Project Timeline

```
Week 1-2:   Project setup, security foundation, database schema
Week 3-4:   User module, authentication, frontend auth flow
Week 5-6:   Account management, transaction module backend
Week 7-8:   Transaction UI, dashboard backend
Week 9-10:  Dashboard UI with animations, goal tracking
Week 11-12: Investment module backend and UI
Week 13-14: Mobile app setup, shared code extraction
Week 15-16: Mobile core features, testing
Week 17-19: AI coach integration
Week 20-22: Household accounts, advanced gamification
Week 23-24: Polish, performance optimization, deployment
```

## Success Criteria

### Phase 1 (Foundation)
- [ ] User can register and login securely
- [ ] JWT authentication working with refresh tokens
- [ ] Database schema created and migrated
- [ ] Docker development environment functional
- [ ] All security configurations in place

### Phase 2 (Core Features)
- [ ] User can create multiple accounts
- [ ] User can add, edit, delete transactions
- [ ] Dashboard shows accurate account summaries
- [ ] Goal tracker shows progress with animations
- [ ] 80%+ test coverage for backend services

### Phase 3 (Investment & Mobile)
- [ ] Investment portfolio tracking functional
- [ ] Mobile app works on iOS and Android
- [ ] Shared code between web and mobile
- [ ] All animations run at 60fps

### Phase 4 (AI & Advanced)
- [ ] AI coach provides spending insights
- [ ] Household accounts work with proper permissions
- [ ] Achievement system motivates users
- [ ] Application handles 1000+ concurrent users

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Security vulnerabilities | Medium | Critical | Security-first design, regular audits, OWASP compliance |
| Financial calculation errors | Medium | High | Extensive unit tests, decimal precision, audit logging |
| Scope creep | High | Medium | Strict MVP definition, phased approach |
| Tech stack complexity | Medium | Medium | Start simple, add complexity as needed |
| AI API costs | Medium | Low | Caching, rate limiting, cost monitoring |

## Next Steps

1. **Review this plan** and confirm approach
2. **Set up development environment** (Docker, IDE)
3. **Start Phase 1** - Project initialization
4. **Establish development rhythm** - Daily commits, weekly reviews

---

*Plan created: 2025-04-02*
*Agent ID: aa1e21f (for resuming planning agent if needed)*
