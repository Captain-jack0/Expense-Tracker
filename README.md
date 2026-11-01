# Personal Finance Tracker & Investment Assistant

A gamified personal finance application (web + mobile) that goes beyond basic expense tracking to provide investment management, goal tracking, and AI-powered financial coaching.

---

## 📚 Documentation

This project includes comprehensive documentation to guide development:

### Core Documentation

| Document | Description | Purpose |
|----------|-------------|---------|
| **[plan/IMPLEMENTATION_PLAN.md](./plan/IMPLEMENTATION_PLAN.md)** | Complete project roadmap | High-level overview, architecture, timeline |
| **[docs/api/README.md](./docs/api/README.md)** | API reference | Auth flow, conventions, error format, endpoints |
| **[frontend/README.md](./frontend/README.md)** | Frontend documentation | React + Vite + TypeScript setup and usage |
| **[plan/SECURITY_GUIDE.md](./plan/SECURITY_GUIDE.md)** | Security guide | Target security design (not all of it is implemented yet — see [Security](#-security)) |

---

## 🚀 Quick Start

### Prerequisites

- Java JDK 17+ and Maven 3.9+
- Node.js 20+ (what CI uses)
- Docker Desktop (only for the optional MySQL stack)
- Git

### 1. Clone and run (local dev — no Docker needed)

The backend runs on an embedded, file-based **H2** database by default
(`backend/data/`), so nothing else has to be installed.

```bash
git clone https://github.com/Captain-jack0/Expense-Tracker.git
cd Expense-Tracker

# Backend (http://localhost:8080). Uses a public dev JWT key and logs a
# warning about it; set APP_JWT_SECRET to use your own.
cd backend
mvn spring-boot:run

# In a new terminal: frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

### 2. Optional: full stack on MySQL with Docker

```bash
cp .env.example .env
# Edit .env and set APP_JWT_SECRET (required), e.g. the output of:
#   openssl rand -base64 48
docker compose up -d     # MySQL + Redis + backend, from the repo root
```

Compose refuses to start without `APP_JWT_SECRET`, so the containerised backend
never signs tokens with the public dev key.

### 3. Access Application

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080 (Swagger UI at `/swagger-ui.html`)
- **H2 console (local dev only):** http://localhost:8080/h2-console
- **MySQL (Docker stack):** localhost:3306
- **Redis (Docker stack):** localhost:6379. The container starts, but the backend does not use Redis yet.

### Database migrations

The schema is managed by **Flyway**. Scripts live in
`backend/src/main/resources/db/migration/{h2,mysql}/` (one folder per database
vendor) and Hibernate only validates against them (`ddl-auto=validate`). To
change the schema, add a new `V<n>__description.sql` to **both** folders. A
database created before Flyway was introduced is adopted automatically: it is
stamped as V1 and only newer migrations are applied.

---

## 🏗️ Tech Stack

### Backend
- **Language:** Java 17 (Kotlin is planned, not used yet)
- **Framework:** Spring Boot 3.3 (Web, Data JPA, Validation, Actuator)
- **Build Tool:** Maven
- **Database:** H2 (local dev, file-based) / MySQL 8.4 (Docker stack)
- **Migrations:** Flyway
- **Cache:** Redis 7 is provisioned in Docker Compose but not used by the code yet
- **Security:** JWT (jjwt, HS256) + BCrypt via `spring-security-crypto`
- **API docs:** springdoc-openapi (Swagger UI)

### Frontend (Web)
- **Framework:** React 19.2.4
- **Build Tool:** Vite 8.0
- **Language:** TypeScript 5.9
- **Routing:** React Router v7
- **Styling:** Tailwind CSS 4.2
- **State Management:** React Query (TanStack Query)
- **API Client:** Axios
- **Animation:** Framer Motion
- **Charts:** Recharts
- **Validation:** Zod
- **Date Handling:** date-fns

### Frontend (Mobile) — planned (Phase 3), not started
- **Framework:** React Native (Expo) for cross-platform
- **Android Native:** Kotlin (planned for native Android app)
- **Animation:** React Native Reanimated
- **Navigation:** React Navigation
- **Backend Integration:** Same Spring Boot API

### DevOps
- **Containerization:** Docker + Docker Compose
- **CI/CD:** GitHub Actions
- **Deployment:** Vercel (web), Railway/Render (backend)

---

## 📋 Project Structure

```
Expense-Tracker/
├── backend/                      # Spring Boot (Java 17) API
│   ├── src/main/java/com/expensetracker/
│   │   ├── config/              # CORS, OpenAPI, password encoder
│   │   ├── controller/          # REST controllers (auth, categories, sub-categories, transactions)
│   │   ├── dto/                 # Request/response DTOs
│   │   ├── exception/           # ApiException + GlobalExceptionHandler
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Spring Data repositories
│   │   ├── security/            # JWT, @CurrentUser resolver, auth rate limiter
│   │   └── service/             # Business logic
│   ├── src/main/resources/
│   │   ├── application*.properties
│   │   └── db/migration/{h2,mysql}/   # Flyway migrations
│   ├── Dockerfile
│   └── pom.xml                  # Maven build configuration
│
├── docker-compose.yml            # MySQL + Redis + backend
├── e2e/                          # Playwright end-to-end tests
├── docs/api/                     # API reference
│
├── frontend/                     # React + Vite + TypeScript web app
│   ├── src/
│   │   ├── components/          # Reusable UI components
│   │   │   ├── auth/           # Authentication components
│   │   │   ├── common/         # Common components
│   │   │   ├── dashboard/      # Dashboard components
│   │   │   ├── transactions/   # Transaction components
│   │   │   ├── goals/          # Goal tracking components
│   │   │   └── investments/    # Investment components
│   │   ├── contexts/            # React Context (Auth, etc.)
│   │   ├── pages/               # Page components
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   └── DashboardPage.tsx
│   │   ├── services/            # API service layer
│   │   │   └── api.ts
│   │   ├── types/               # TypeScript definitions
│   │   │   └── index.ts
│   │   ├── hooks/               # Custom React hooks
│   │   ├── utils/               # Utility functions
│   │   ├── App.tsx              # Main app + routing
│   │   ├── main.tsx             # Entry point
│   │   └── index.css            # Tailwind styles
│   ├── public/                  # Static assets
│   ├── .env                     # Environment variables
│   ├── package.json
│   ├── tsconfig.json            # TypeScript config
│   ├── vite.config.ts           # Vite config
│   └── tailwind.config.js       # Tailwind config
│
├── mobile/                       # React Native app (Phase 3)
│   └── (to be implemented)
│
├── plan/                         # Project documentation
│   └── IMPLEMENTATION_PLAN.md
│
└── README.md                     # This file
```

---

## ✨ Features

### ✅ Phase 1 (Completed) - Weeks 1-4
- ✅ React + Vite + TypeScript project setup
- ✅ Authentication system (Login, Register)
- ✅ Protected routes with JWT
- ✅ API service layer with token refresh
- ✅ Basic dashboard UI
- ✅ Tailwind CSS integration
- ✅ TypeScript strict mode

### 🔄 Phase 2 (In Progress) - Weeks 5-10
- ✅ Income/expense tracking — categorized transactions API (`/api/transactions`) with filters
- ✅ Category system — categories + sub-categories with monthly limits (`/api/categories`, `/api/subcategories`)
- 🟡 Recurring & installment (*taksitli*) — data model shipped (schema-reserved fields); generation engine not yet implemented (see [docs/api/README.md](./docs/api/README.md#7-recurring--installment-behavior))
- 📅 Multiple account management (CRUD)
- 📅 Goal tracking with progress visualization
- 📅 Dashboard with animated charts (Recharts)
- 📅 Basic investment tracking

> **API reference:** see [docs/api/README.md](./docs/api/README.md) and the live Swagger UI at `/swagger-ui.html`.

### ⏳ Phase 3 (Planned) - Weeks 11-16
- 📅 Investment portfolio tracking
- 📅 React Native mobile app
- 📅 60-70% code sharing between web/mobile

### 🔮 Phase 4 (Future) - Weeks 17-24
- 📅 AI financial coach (OpenAI integration)
- 📅 Household accounts
- 📅 Achievement system
- 📅 Advanced gamification

---

## 🔒 Security

**Implemented today:**

- **Authentication:** JWT access tokens (15 min) + refresh tokens (7 days). Logout revokes all of the user's refresh tokens (per-user token version).
- **Signing key:** `APP_JWT_SECRET`, which is required by the Docker profile (no fallback). Local dev uses a public key and logs a warning.
- **Password storage:** BCrypt (Spring default cost 10). Unknown-email logins still run a BCrypt check, so timing doesn't reveal which accounts exist.
- **Brute-force protection:** in-memory rate limit on login/register (10 attempts per 5 min, then HTTP 429).
- **Authorization:** every resource query is scoped to the authenticated user.
- **Error handling:** consistent JSON error envelope. Client errors return 4xx and internals are never leaked.
- **Actuator:** only `/actuator/health` is exposed, with no details.
- **CI:** CodeQL, npm audit, OWASP dependency-check (weekly), TruffleHog secret scanning, Dependabot.

**Planned, not implemented yet** (see [plan/SECURITY_GUIDE.md](./plan/SECURITY_GUIDE.md)):
AES-256-GCM field encryption, TOTP 2FA, audit logging, a Redis-backed
(multi-instance) rate limiter, a Spring Security filter chain, and moving tokens
from `localStorage` to httpOnly cookies.

---

## 🎨 Gamification

### Achievements
- 🌱 Getting Started (first transaction)
- 🔥 Week Warrior (7-day streak)
- 🎯 Goal Setter (create first goal)
- 🏆 Goal Crusher (complete goal)
- 💰 Budget Master (stay under budget)
- 📈 Portfolio Builder (10 investments)

### Visual Feedback
- Animated progress rings
- Growing plant metaphor for savings
- Confetti on goal completion
- Streak fire animation
- Level progression system

---

## 📱 Mobile Development

### Code Sharing Strategy
60-70% of code shared between web and mobile:

- **Shared:** Types, validation, API services, business logic
- **Platform-specific:** UI components, navigation, animations
- **Security:** Secure storage (Expo SecureStore) for mobile tokens

---

## 🧪 Testing

### Coverage gate: 80% (enforced in CI for both backend and frontend)

- **Backend:** JUnit 5 + Mockito service tests, plus MockMvc integration tests that run against the real Flyway-migrated H2 schema (`ApiIntegrationTest`). JaCoCo enforces 80% line coverage in `mvn verify`.
- **Frontend:** Vitest + Testing Library, with an 80% threshold on lines, branches, functions and statements.
- **E2E:** Playwright smoke tests in `e2e/` (run on demand in CI).

```bash
# Backend tests + coverage gate
cd backend
mvn verify

# Frontend tests + coverage gate
cd frontend
npm run test:coverage

# E2E (needs backend on :8080 and `npm run preview` on :4173)
cd e2e
npm install
npx playwright test
```

---

## 📈 Development Roadmap

| Week | Focus | Status |
|------|-------|--------|
| 1-2 | Backend setup, security foundation | ⏳ In Progress |
| 3-4 | Frontend setup, authentication | ✅ Completed |
| 5-6 | Account & transaction management | 📅 Planned |
| 7-8 | Dashboard, charts, animations | 📅 Planned |
| 9-10 | Goal tracking | 📅 Planned |
| 11-12 | Investment module | 📅 Planned |
| 13-14 | Mobile app setup | 📅 Planned |
| 15-16 | Mobile core features | 📅 Planned |
| 17-19 | AI coach | 🔮 Future |
| 20-22 | Household accounts | 🔮 Future |
| 23-24 | Polish & deployment | 🔮 Future |

### Current Status: Phase 2 in progress 🔄

**Completed:**
- ✅ Frontend on Vite + React + TypeScript, with login/register and protected routes
- ✅ Spring Boot backend with JWT auth (register, login, refresh, logout with revocation)
- ✅ Categories and sub-categories (API + UI), default categories seeded on registration
- ✅ Transactions API (CRUD, filters, pagination)
- ✅ Flyway migrations, MySQL Docker stack, CI with coverage gates and security scans

**Next Steps:**
- Transaction UI and a real dashboard (the dashboard currently shows placeholder values)
- Account management (CRUD)
- Recurring and installment transaction engines
- Goal tracking

---

## 🤝 Contributing

This is a personal learning project, but contributions are welcome!

1. Follow the coding standards in the documentation
2. Write tests for new features
3. Update documentation as needed
4. Follow the security guidelines

---

## 📄 License

This project is for educational purposes.

---

## 🔗 Resources

### Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [Vite Documentation](https://vite.dev)
- [React Router Documentation](https://reactrouter.com)
- [Tailwind CSS Documentation](https://tailwindcss.com)

### Database & Security
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Flyway Documentation](https://documentation.red-gate.com/flyway)
- [Redis Documentation](https://redis.io/docs/)
- [JWT Introduction](https://jwt.io/introduction)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

### Animation & UI
- [Framer Motion](https://www.framer.com/motion/)
- [Recharts](https://recharts.org/)
- [React Query](https://tanstack.com/query/latest)

---

## 💡 Next Steps

1. **Read [plan/IMPLEMENTATION_PLAN.md](./plan/IMPLEMENTATION_PLAN.md)** - Understand the full scope
2. **Review [docs/api/README.md](./docs/api/README.md)** - API conventions and endpoints
3. **Review [frontend/README.md](./frontend/README.md)** - Frontend setup and architecture
4. **Continue Phase 2** - Transaction UI, dashboard, account management

---

## 📞 Support

If you encounter issues:

1. **Check documentation:** `frontend/README.md`, `plan/IMPLEMENTATION_PLAN.md`
2. **Review error logs:** `docker compose logs -f` (Docker stack) or the `mvn spring-boot:run` console
3. **Verify environment variables:** `.env` exists and `APP_JWT_SECRET` is set (Docker stack)
4. **Ensure Docker services running:** `docker ps`
5. **Check Node version:** `node --version` (requires 20+)

### Common Issues

**Frontend won't start:**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

**Database connection failed (Docker stack):**
```bash
# from the repo root
docker compose down
docker compose up -d
```

**`APP_JWT_SECRET must be set` when running `docker compose up`:**
copy `.env.example` to `.env` and set `APP_JWT_SECRET` (e.g. `openssl rand -base64 48`).

---

**Built with ❤️ as a learning project to master full-stack development, security, and modern DevOps practices.**
