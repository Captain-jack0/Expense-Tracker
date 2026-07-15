# Expense Tracker — API Documentation

Reference for the Expense Tracker REST API: conventions, authentication, error
format, versioning, and per-endpoint request/response shapes.

> **Live, always-in-sync docs:** the backend serves an auto-generated OpenAPI 3
> spec and interactive UI (see [Interactive docs & tooling](#interactive-docs--tooling)).
> This Markdown file is the human-readable overview and the source of truth for
> **conventions** (auth flow, error format, versioning) that the generated spec
> doesn't narrate.

---

## 1. Overview

- **Base URL (local):** `http://localhost:8080`
- **API root:** all endpoints are under `/api`
- **Format:** JSON request and response bodies (`Content-Type: application/json`)
- **Auth:** stateless JWT (Bearer access token + refresh token) — see [Authentication](#3-authentication)
- **Dates:** ISO-8601 strings (`2026-07-15T12:00:00Z` for timestamps, `2026-07-15` for dates)
- **CORS:** the Vite dev origins (`http://localhost:5173`, `http://127.0.0.1:5173`) are allowed by default; override with `APP_CORS_ALLOWED_ORIGINS`.

---

## 2. Response conventions

### Envelope (auth endpoints)

The `/api/auth/*` endpoints wrap their payload in a standard envelope:

```json
{
  "success": true,
  "data":    { "...": "endpoint-specific payload" },
  "error":   null,
  "message": null
}
```

| Field     | Type            | Notes                                             |
|-----------|-----------------|---------------------------------------------------|
| `success` | boolean         | `true` on success, `false` on error               |
| `data`    | object \| null  | The payload on success; `null` on error           |
| `error`   | string \| null  | Human-readable error message on failure           |
| `message` | string \| null  | Optional info message (e.g. "Logged out")         |

### Raw payloads (resource endpoints)

> ⚠️ **Current state:** the resource controllers (`/api/expenses`,
> `/api/categories`, `/api/subcategories`, `/api/transactions`) currently return
> the entity or array **directly**, *not* wrapped in the envelope above. The
> envelope is the intended standard; aligning the resource endpoints to it is
> tracked as backend follow-up work. Documented here as-is so integrators aren't
> surprised.

---

## 3. Authentication

JWT-based, stateless. Two tokens are issued:

| Token           | Lifetime | Sent as                              | Used for                    |
|-----------------|----------|--------------------------------------|-----------------------------|
| `accessToken`   | 1 hour   | `Authorization: Bearer <accessToken>`| Every authenticated request |
| `refreshToken`  | 7 days   | request body to `/api/auth/refresh`  | Getting a fresh access token|

**Flow**

1. `POST /api/auth/register` or `POST /api/auth/login` → returns `{ user, accessToken, refreshToken }`.
2. Send `Authorization: Bearer <accessToken>` on protected calls (e.g. `GET /api/auth/me`).
3. When the access token expires (401), call `POST /api/auth/refresh` with the refresh token to get a new pair.
4. `POST /api/auth/logout` is a client-side discard (tokens are stateless); it returns 200 for a stable contract.

Passwords are hashed with **BCrypt**; the hash is never returned. There is no
Spring Security filter chain yet — `GET /api/auth/me` validates the Bearer token
manually. Resource endpoints are **not yet auth-protected** (backend follow-up).

---

## 4. Error handling

All errors handled by `GlobalExceptionHandler` render as the failure envelope:

```json
{ "success": false, "data": null, "error": "Invalid email or password", "message": null }
```

| Status | When                                                            |
|--------|-----------------------------------------------------------------|
| `400`  | Bean-validation failure (returns the first field message), or malformed/unreadable JSON body |
| `401`  | Bad credentials, missing/invalid/expired token, invalid token type |
| `403`  | Forbidden (`ApiException.forbidden`)                            |
| `404`  | Resource not found                                              |
| `409`  | Conflict — e.g. email already registered                       |
| `500`  | Unhandled server error (generic message; details are logged server-side, never leaked) |

---

## 5. Versioning

- **Current:** `v1` (the OpenAPI `info.version`). The API surface lives under `/api`
  with no version path segment yet.
- **Strategy going forward:** introduce **URI versioning** (`/api/v1/...`) at the
  first breaking change. Non-breaking additions (new fields, new endpoints) ship
  without a version bump. When `/api/v2` lands, `/api/v1` is kept until clients
  migrate, then deprecated with a sunset window.

---

## 6. Endpoints

### Auth — `/api/auth`

| Method | Path        | Auth | Body                                   | Returns (in envelope)          |
|--------|-------------|------|----------------------------------------|--------------------------------|
| POST   | `/register` | —    | `RegisterRequest`                      | `AuthResponse` (201)           |
| POST   | `/login`    | —    | `LoginRequest`                         | `AuthResponse` (200)           |
| GET    | `/me`       | ✅   | —                                      | `UserDto` (200)                |
| POST   | `/refresh`  | —    | `{ "refreshToken": "..." }`            | `AuthResponse` (200)           |
| POST   | `/logout`   | —    | —                                      | `null` + message (200)         |

### Expenses — `/api/expenses` *(raw payloads)*

| Method | Path                        | Body      | Returns              |
|--------|-----------------------------|-----------|----------------------|
| GET    | `/`                         | —         | `Expense[]`          |
| GET    | `/{id}`                     | —         | `Expense` / 404      |
| POST   | `/`                         | `Expense` | `Expense` (201)      |
| PUT    | `/{id}`                     | `Expense` | `Expense` / 404      |
| DELETE | `/{id}`                     | —         | 204                  |
| GET    | `/category/{category}`      | —         | `Expense[]`          |
| GET    | `/date-range?startDate&endDate` | —     | `Expense[]` (ISO dates) |
| GET    | `/categories`               | —         | `string[]`           |

### Categories — `/api/categories` *(raw payloads)*

| Method | Path      | Returns       |
|--------|-----------|---------------|
| GET    | `/`       | `Category[]`  |
| POST   | `/`       | `Category`    |
| PUT    | `/{id}`   | `Category`    |
| DELETE | `/{id}`   | 204           |

### SubCategories — `/api/subcategories` *(raw payloads)*

| Method | Path      | Returns          |
|--------|-----------|------------------|
| GET    | `/`       | `SubCategory[]`  |
| POST   | `/`       | `SubCategory`    |
| PUT    | `/{id}`   | `SubCategory`    |
| DELETE | `/{id}`   | 204              |

### Transactions — `/api/transactions` *(raw payloads)*

| Method | Path      | Returns          |
|--------|-----------|------------------|
| GET    | `/`       | `Transaction[]`  |
| POST   | `/`       | `Transaction`    |
| PUT    | `/{id}`   | `Transaction`    |
| DELETE | `/{id}`   | 204              |

---

## 7. Example payloads

### Register

`POST /api/auth/register`

```json
{
  "email": "ada@example.com",
  "password": "correcthorse",
  "firstName": "Ada",
  "lastName": "Lovelace"
}
```

Validation: `email` valid, `password` ≥ 8 chars, `firstName`/`lastName` non-blank.

**201 Created**

```json
{
  "success": true,
  "data": {
    "user": {
      "id": "4de9342a-ca41-4865-8813-0772d7eb1fcb",
      "email": "ada@example.com",
      "firstName": "Ada",
      "lastName": "Lovelace",
      "currency": "USD",
      "timezone": "UTC",
      "isActive": true,
      "emailVerified": false,
      "createdAt": "2026-07-15T12:00:00Z",
      "updatedAt": "2026-07-15T12:00:00Z"
    },
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  },
  "error": null,
  "message": null
}
```

### Create an expense

`POST /api/expenses`  (raw payload — not enveloped)

```json
{
  "title": "Groceries",
  "amount": 42.50,
  "category": "Food",
  "description": "Weekly shop",
  "date": "2026-07-15"
}
```

**201 Created**

```json
{
  "id": 1,
  "title": "Groceries",
  "amount": 42.50,
  "category": "Food",
  "description": "Weekly shop",
  "date": "2026-07-15",
  "createdAt": "2026-07-15"
}
```

---

## 8. Common scenarios

**Sign in and call a protected endpoint**

```bash
# 1. Log in, capture the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@example.com","password":"correcthorse"}' \
  | jq -r '.data.accessToken')

# 2. Use it
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

**Refresh an expired token**

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

**Filter expenses by date range**

```bash
curl -s "http://localhost:8080/api/expenses/date-range?startDate=2026-07-01&endDate=2026-07-31"
```

---

## Interactive docs & tooling

The backend auto-generates the spec from the controllers via **springdoc-openapi**,
so it never drifts from the code.

| Resource        | URL (local)                          |
|-----------------|--------------------------------------|
| Swagger UI      | `http://localhost:8080/swagger-ui.html` |
| OpenAPI 3 spec  | `http://localhost:8080/v3/api-docs`  |

- **Swagger UI:** click **Authorize**, paste an `accessToken` (from `/api/auth/login`),
  and try any endpoint in-browser.
- **Postman:** *Import → Link →* `http://localhost:8080/v3/api-docs`. Postman builds
  a collection from the OpenAPI spec automatically.
- **Metadata & the JWT security scheme** are defined in
  [`OpenApiConfig.java`](../../backend/src/main/java/com/expensetracker/config/OpenApiConfig.java).

---

## Where docs live

```
docs/
└── api/
    └── README.md        ← this file (conventions + overview; source of truth for narrative)
backend/.../config/OpenApiConfig.java   ← OpenAPI metadata + JWT scheme
/v3/api-docs, /swagger-ui.html          ← generated per-endpoint reference (run the backend)
```

Keep **conventions** (auth, errors, versioning) here; let **per-endpoint detail**
come from the generated spec. When you add or change an endpoint, the spec updates
automatically — only touch this file if a *convention* changes.
