# E2E Tests

End-to-end tests for the Expense Tracker application using [Playwright](https://playwright.dev/).
Tests exercise the real frontend (Vite preview build) hitting the real Spring Boot backend.

## Running locally

You need **three terminals** — one for each service and one for the tests.

**Terminal A — backend:**
```bash
cd backend
mvn spring-boot:run
# Wait until you see "Started ExpenseTrackerApplication"
```

**Terminal B — frontend preview (production build):**
```bash
cd frontend
npm run build
npx vite preview --port 4173
```

**Terminal C — Playwright tests:**
```bash
cd e2e
npm install            # first time only
npx playwright install chromium   # first time only
npm test
```

## Useful commands

| Command | What it does |
|---|---|
| `npm test` | Run all tests headlessly (chromium) |
| `npm run test:ui` | Interactive UI mode (great for debugging) |
| `npm run test:headed` | Run with a visible browser window |
| `npm run test:debug` | Step through tests with the Playwright inspector |
| `npm run report` | Open the HTML report from the last run |

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `BASE_URL` | `http://127.0.0.1:4173` | Frontend origin used by `page.goto('/...')` |
| `API_URL` | `http://localhost:8080` | Backend origin used by `request.get(...)` |

## CI

Tests run automatically on pull requests when `backend/**`, `frontend/**`, or `e2e/**` changes.
You can also force a run by adding the `e2e` label to your PR, or by triggering
`Actions → E2E Tests → Run workflow` manually.

On failure, the workflow uploads three artifacts:

- `playwright-report` — HTML report (always)
- `playwright-traces` — full traces with screenshots & videos (failure only)
- `server-logs` — backend & frontend startup logs (failure only)

## Adding new tests

Add `*.spec.ts` files under `tests/`. Keep tests fast and isolated — each test
should not depend on state left behind by a previous test.

The current smoke suite only verifies that both services start and serve
their root pages. As backend features (auth, expenses CRUD, etc.) land,
extend the suite with real user-flow tests (register → login → add expense
→ see it on the dashboard).
