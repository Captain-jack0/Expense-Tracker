# Personal Finance Tracker & Investment Assistant

A gamified personal finance application (web + mobile) that goes beyond basic expense tracking to provide investment management, goal tracking, and AI-powered financial coaching.

---

## 📚 Documentation

This project includes comprehensive documentation to guide development:

### Core Documentation

| Document | Description | Purpose |
|----------|-------------|---------|
| **[IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)** | Complete project roadmap | High-level overview, architecture, timeline |
| **[PROJECT_CHECKLIST.md](./PROJECT_CHECKLIST.md)** | Master task checklist | Track all tasks and milestones |
| **[SECURITY_GUIDE.md](./SECURITY_GUIDE.md)** | Security implementation | JWT, encryption, OWASP Top 10, 2FA |
| **[FRONTEND_UX_GUIDE.md](./FRONTEND_UX_GUIDE.md)** | Frontend & animations | Framer Motion, gamification, mobile |
| **[PHASE_1_IMPLEMENTATION.md](./PHASE_1_IMPLEMENTATION.md)** | Step-by-step setup | Week 1-4 detailed instructions |
| **[N8N_DEPLOYMENT.md](./N8N_DEPLOYMENT.md)** | Workflow automation | n8n Docker setup, automated workflows |

---

## 🚀 Quick Start

### Prerequisites

- Java JDK 17+
- Node.js 18+
- Docker Desktop
- Git

### 1. Clone and Setup

```bash
#To start planning html
python -m http.server 8000

cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker"

# Start database and cache
cd backend
docker-compose up -d

# Build and run backend
./gradlew bootRun

# In new terminal, start frontend
cd ../frontend
npm install
npm run dev
```

### 2. Access Application

- **Planner** http://localhost:8000
- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **PostgreSQL:** localhost:5432
- **Redis:** localhost:6379
- **n8n Automation:** http://localhost:5678

---

## 🏗️ Tech Stack

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.3
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Security:** JWT + bcrypt

### Frontend (Web)
- **Framework:** React 18
- **Build Tool:** Vite
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **Animation:** Framer Motion, Lottie
- **Charts:** Recharts

### Frontend (Mobile)
- **Framework:** React Native (Expo)
- **Animation:** React Native Reanimated
- **Navigation:** React Navigation

### DevOps
- **Containerization:** Docker
- **CI/CD:** GitHub Actions
- **Deployment:** Vercel (web), Railway (backend)
- **Workflow Automation:** n8n (Docker)

---

## 📋 Project Structure

```
Expense Tracker/
├── backend/                      # Spring Boot + Java API
│   ├── src/main/java/
│   │   └── com/financetracker/
│   │       ├── common/          # Security, config, utils
│   │       ├── user/            # User & auth module
│   │       ├── transaction/     # Transaction module
│   │       ├── goal/            # Goal tracking module
│   │       ├── investment/      # Investment module
│   │       └── coach/           # AI coach (Phase 4)
│   ├── docker-compose.yml
│   └── build.gradle.kts
│
├── frontend/                     # React web application
│   ├── src/
│   │   ├── components/          # Reusable components
│   │   ├── pages/               # Route pages
│   │   ├── lib/                 # API client, utilities
│   │   ├── contexts/            # React contexts
│   │   └── hooks/               # Custom hooks
│   └── package.json
│
├── mobile/                       # React Native app
│   ├── app/                     # Expo Router screens
│   ├── components/              # Mobile components
│   └── lib/                     # Shared logic
│
├── shared/                       # Shared code (types, validation, API)
│   └── src/
│       ├── types/
│       ├── validation/
│       └── api/
│
├── IMPLEMENTATION_PLAN.md
├── SECURITY_GUIDE.md
├── FRONTEND_UX_GUIDE.md
├── PHASE_1_IMPLEMENTATION.md
└── README.md (this file)
```

---

## ✨ Features

### Phase 1-2 (MVP) - Weeks 1-10
-  User authentication with JWT
- 📅 Multiple account management
- 📅 Income/expense tracking
- 📅 Category system
- 📅 Goal tracking with progress visualization
- 📅 Dashboard with animated charts
- 📅 Basic investment tracking

### Phase 3 - Weeks 11-16
- 📅 Investment portfolio tracking
- 📅 React Native mobile app
- 📅 Code sharing between web/mobile

### Phase 4 - Weeks 17-24
- 📅 AI financial coach
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
# Run backend tests
cd backend
./gradlew test

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
| 1-2 | Backend setup, security foundation | 📅 Planned |
| 3-4 | User auth, JWT, frontend setup | 📅 Planned |
| 5-6 | Account & transaction management | 📅 Planned |
| 7-8 | Dashboard, charts, animations | 📅 Planned |
| 9-10 | Goal tracking | 📅 Planned |
| 11-12 | Investment module | 📅 Planned |
| 13-14 | Mobile app setup | 📅 Planned |
| 15-16 | Mobile core features | 📅 Planned |
| 17-19 | AI coach | 📅 Future |
| 20-22 | Household accounts | 📅 Future |
| 23-24 | Polish & deployment | 📅 Future |

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

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [React Native Documentation](https://reactnative.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JWT Introduction](https://jwt.io/introduction)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

---

## 💡 Next Steps

1. **Open [PROJECT_CHECKLIST.md](./PROJECT_CHECKLIST.md)** - Use this as your daily task tracker
2. **Read [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)** - Understand the full scope
3. **Review [SECURITY_GUIDE.md](./SECURITY_GUIDE.md)** - Learn security practices
4. **Follow [PHASE_1_IMPLEMENTATION.md](./PHASE_1_IMPLEMENTATION.md)** - Start building
5. **Check [FRONTEND_UX_GUIDE.md](./FRONTEND_UX_GUIDE.md)** - Implement animations

---

## 📞 Support

If you encounter issues:
1. Check the documentation files
2. Review error logs: `docker-compose logs -f`
3. Verify environment variables are set
4. Ensure Docker services are running: `docker ps`

---

**Built with ❤️ as a learning project to master full-stack development, security, and modern DevOps practices.**
