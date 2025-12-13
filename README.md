# Expense Tracker

A full-stack expense tracking application built with React Native Web (cross-platform) and Java Spring Boot.

## Project Structure

```
Expense-Tracker/
├── frontend/           # React Native Web frontend
│   ├── src/
│   │   ├── components/ # Reusable UI components
│   │   ├── screens/    # Screen components
│   │   ├── services/   # API services
│   │   └── App.js      # Main application
│   └── public/         # Static files
├── backend/            # Java Spring Boot backend
│   ├── src/main/java/com/expensetracker/
│   │   ├── controller/ # REST controllers
│   │   ├── model/      # Entity models
│   │   ├── repository/ # Data repositories
│   │   └── service/    # Business logic
│   └── pom.xml         # Maven configuration
└── README.md
```

## Features

- Add, edit, and delete expenses
- Categorize expenses (Food, Transport, Entertainment, etc.)
- View total expenses
- Filter by category and date range
- Responsive design for web and mobile

## Prerequisites

- Node.js 18+ and npm
- Java 17+
- Maven 3.6+

## Getting Started

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Build and run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

   The API will be available at `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

   The app will open at `http://localhost:3000`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/expenses` | Get all expenses |
| GET | `/api/expenses/{id}` | Get expense by ID |
| POST | `/api/expenses` | Create new expense |
| PUT | `/api/expenses/{id}` | Update expense |
| DELETE | `/api/expenses/{id}` | Delete expense |
| GET | `/api/expenses/category/{category}` | Get by category |
| GET | `/api/expenses/categories` | Get all categories |
| GET | `/api/expenses/date-range` | Get by date range |

## Mobile Support

This project uses React Native Web, which allows the same codebase to run on:
- Web browsers
- iOS (with Expo)
- Android (with Expo)

To run on mobile with Expo:
```bash
cd frontend
npx expo start
```

## Database

By default, the application uses H2 in-memory database for development.

For production, configure MySQL in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/expensedb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## License

MIT
