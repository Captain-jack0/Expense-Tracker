# Personal Finance Tracker & Investment Assistant

A gamified personal finance application (web + mobile) that goes beyond basic expense tracking to provide investment management, goal tracking, and AI-powered financial coaching.

---

## 📚 Documentation

This project includes comprehensive documentation to guide development:

### Core Documentation

| Document | Description | Purpose |
|----------|-------------|---------|
| **[plan/IMPLEMENTATION_PLAN.md](./plan/IMPLEMENTATION_PLAN.md)** | Complete project roadmap | High-level overview, architecture, timeline |
| **[frontend/README.md](./frontend/README.md)** | Frontend documentation | React + Vite + TypeScript setup and usage |

---

## 🚀 Quick Start

### Prerequisites

- Java JDK 17+
- Node.js 18+
- Docker Desktop
- Git

### 1. Clone and Setup

```bash
cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker\Expense-Tracker"

# Start database and cache
cd backend
docker-compose up -d

# Build and run backend (Maven)
mvn spring-boot:run
# OR for Windows:
# mvnw.cmd spring-boot:run

# In new terminal, start frontend
cd ../frontend
npm install
npm run dev
```

### 2. Access Application

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **PostgreSQL:** localhost:5432
- **Redis:** localhost:6379

---

## 🏗️ Tech Stack

### Backend
- **Languages:** Kotlin + Java (Hybrid)
  - **Java:** Existing/legacy code, production-proven features
  - **Kotlin:** New features, modern implementations
  - **Interoperability:** Full compatibility between Java and Kotlin
- **Framework:** Spring Boot 3.2
- **Build Tool:** Maven
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Security:** JWT + bcrypt
- **Build Tool:** Gradle

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

### Frontend (Mobile)
- **Framework:** React Native (Expo) for cross-platform
- **Android Native:** Kotlin (planned for native Android app)
- **Animation:** React Native Reanimated
- **Navigation:** React Navigation
- **Backend Integration:** Same Spring Boot API (Java + Kotlin)

### DevOps
- **Containerization:** Docker + Docker Compose
- **CI/CD:** GitHub Actions
- **Deployment:** Vercel (web), Railway/Render (backend)

---

## 📋 Project Structure

```
Expense Tracker/
├── backend/                      # Spring Boot + Kotlin + Java API
│   ├── src/main/java/           # Java source (existing/legacy)
│   │   └── com/expensetracker/
│   │       ├── controller/      # REST controllers
│   │       ├── model/           # Domain models
│   │       ├── repository/      # Data access
│   │       └── service/         # Business logic
│   ├── src/main/kotlin/         # Kotlin source (new features)
│   │   └── com/expensetracker/
│   │       ├── controller/      # REST controllers
│   │       ├── model/           # Domain models
│   │       ├── repository/      # Data access
│   │       └── service/         # Business logic
│   ├── docker-compose.yml
│   └── pom.xml                  # Maven build configuration
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
- 📅 Multiple account management (CRUD)
- 📅 Income/expense tracking
- 📅 Category system
- 📅 Goal tracking with progress visualization
- 📅 Dashboard with animated charts (Recharts)
- 📅 Basic investment tracking

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

This application implements industry-standard security practices:

- **Authentication:** JWT with refresh tokens
- **Password Storage:** bcrypt (cost factor 12)
- **Data Encryption:** AES-256-GCM for sensitive fields
- **Rate Limiting:** Redis-based protection
- **2FA:** TOTP-based two-factor authentication
- **OWASP Compliance:** Top 10 checklist implemented
- **Audit Logging:** All financial operations tracked

**See [SECURITY_GUIDE.md](./SECURITY_GUIDE.md) for complete security documentation.**

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

### Coverage Target: 80%+

- **Unit Tests:** JUnit 5 (backend), Vitest (frontend)
- **Integration Tests:** TestContainers (backend), MSW (frontend)
- **E2E Tests:** Playwright for critical user flows

```bash
# Run backend tests (Maven)
cd backend
mvn test
# OR for Windows:
# mvnw.cmd test

# Run frontend tests
cd frontend
npm run test

# Run E2E tests
cd frontend
npm run test:e2e
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

### Current Status: Phase 1 Complete ✅

**Completed:**
- ✅ Frontend migration to Vite + React + TypeScript
- ✅ Authentication system (Login, Register, Protected Routes)
- ✅ JWT token management with automatic refresh
- ✅ Centralized API service layer
- ✅ Modern UI with Tailwind CSS

**Next Steps:**
- Backend Spring Boot setup
- PostgreSQL + Redis integration
- User registration & authentication endpoints
- Account management (CRUD)
- Transaction tracking

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
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
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
2. **Review [frontend/README.md](./frontend/README.md)** - Frontend setup and architecture
3. **Start Backend Development** - Spring Boot + Kotlin setup
4. **Implement Phase 2** - Account & transaction management

---

## 📞 Support

If you encounter issues:

1. **Check documentation:** `frontend/README.md`, `plan/IMPLEMENTATION_PLAN.md`
2. **Review error logs:** `docker-compose logs -f`
3. **Verify environment variables:** `.env` files are configured
4. **Ensure Docker services running:** `docker ps`
5. **Check Node version:** `node --version` (requires 18+)

### Common Issues

**Frontend won't start:**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

**Database connection failed:**
```bash
cd backend
docker-compose down
docker-compose up -d
```

---

**Built with ❤️ as a learning project to master full-stack development, security, and modern DevOps practices.**
