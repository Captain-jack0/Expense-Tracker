# Expense Tracker - Frontend

Modern web-based personal finance tracker built with React, TypeScript, and Vite.

## 🚀 Tech Stack

- **Framework:** React 19.2.4
- **Language:** TypeScript 5.9
- **Build Tool:** Vite 8.0
- **Routing:** React Router v7
- **Styling:** Tailwind CSS 4.2
- **State Management:** React Query (TanStack Query)
- **API Client:** Axios
- **Animations:** Framer Motion
- **Charts:** Recharts
- **Validation:** Zod
- **Date Handling:** date-fns

## 📁 Project Structure

```
frontend/
├── src/
│   ├── components/        # Reusable UI components
│   │   ├── auth/         # Authentication components
│   │   ├── common/       # Common components
│   │   ├── dashboard/    # Dashboard components
│   │   ├── transactions/ # Transaction components
│   │   ├── goals/        # Goal tracking components
│   │   └── investments/  # Investment components
│   ├── contexts/         # React Context providers
│   │   └── AuthContext.tsx
│   ├── pages/            # Page components
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── DashboardPage.tsx
│   ├── services/         # API service layer
│   │   └── api.ts
│   ├── types/            # TypeScript type definitions
│   │   └── index.ts
│   ├── utils/            # Utility functions
│   ├── hooks/            # Custom React hooks
│   ├── App.tsx           # Main App component
│   ├── main.tsx          # Application entry point
│   └── index.css         # Global styles
├── public/               # Static assets
├── .env                  # Environment variables
├── .env.example          # Environment variables template
├── package.json          # Dependencies and scripts
├── tsconfig.json         # TypeScript configuration
├── tailwind.config.js    # Tailwind CSS configuration
└── vite.config.ts        # Vite configuration
```

## 🛠️ Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend server running on `http://localhost:8080`

### Installation

1. Install dependencies:
```bash
npm install
```

2. Configure environment variables:
```bash
cp .env.example .env
```

Edit `.env` and set the backend API URL:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

### Development

Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Build for Production

```bash
npm run build
```

Production files will be in the `dist/` directory.

### Preview Production Build

```bash
npm run preview
```

## 🔐 Authentication

The app uses JWT-based authentication with the following flow:

1. User logs in or registers
2. Server returns access token and refresh token
3. Access token is sent with every API request
4. Refresh token is used to get a new access token when expired
5. Tokens are stored in `localStorage`

### Protected Routes

Routes under the `/dashboard` path require authentication. Unauthenticated users are redirected to `/login`.

## 📡 API Integration

All API calls are centralized in `src/services/api.ts` using Axios:

- **Base URL:** Configured via `VITE_API_BASE_URL` environment variable
- **Auth:** JWT token automatically added to all requests
- **Token Refresh:** Automatic token refresh on 401 responses
- **Error Handling:** Centralized error handling with interceptors

### Available APIs

- `authApi` - Login, register, logout, user profile
- `accountsApi` - Account management (CRUD)
- `transactionsApi` - Transaction management (CRUD)
- `goalsApi` - Goal tracking (CRUD)
- `categoriesApi` - Category management (CRUD)
- `investmentsApi` - Investment tracking (CRUD)
- `dashboardApi` - Dashboard data aggregation

## 🎨 Styling

The app uses Tailwind CSS with custom utility classes defined in `index.css`:

- `.btn` - Base button styles
- `.btn-primary`, `.btn-secondary`, `.btn-success`, `.btn-danger` - Button variants
- `.card` - Card container styles
- `.input` - Form input styles

### Color Palette

- **Primary:** Blue (`#3b82f6`)
- **Success:** Green (`#27ae60`)
- **Danger:** Red (`#e74c3c`)
- **Warning:** Orange (`#f39c12`)

## 📝 TypeScript Types

All TypeScript types are defined in `src/types/index.ts`:

- **Entities:** User, Account, Transaction, Goal, Investment, Category
- **API Requests/Responses:** LoginRequest, RegisterRequest, AuthResponse, etc.
- **Enums:** AccountType, TransactionType, GoalStatus, InvestmentType
- **UI State:** LoadingState, FormState

## 🧪 Testing (To Be Implemented)

- **Unit Tests:** Vitest + React Testing Library
- **E2E Tests:** Playwright
- **Target Coverage:** 80%+

## 🚧 Current Status

✅ **Phase 1 Completed:**
- React + Vite + TypeScript project setup
- Authentication system (Login, Register, Protected Routes)
- API service layer with JWT authentication
- Basic dashboard UI
- Tailwind CSS integration

⏳ **Next Steps (Phase 2):**
- Account management (CRUD)
- Transaction tracking (income/expense)
- Category system
- Dashboard with animated charts
- Goal tracker with progress visualization

## 📦 Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## 🔗 Related

- Backend: `../backend/` - Spring Boot + Kotlin backend
- Implementation Plan: `../plan/IMPLEMENTATION_PLAN.md`

## 📄 License

Private project - not for public use.
