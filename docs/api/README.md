# Expense Tracker — API Documentation

Reference for the Expense Tracker REST API: conventions, authentication, data
model, error format, versioning, and per-endpoint request/response shapes.

> **Live, always-in-sync docs:** the backend serves an auto-generated OpenAPI 3
> spec and interactive UI (see [Interactive docs & tooling](#interactive-docs--tooling)).
> This Markdown file is the human-readable overview and the source of truth for
> **conventions** (auth flow, data model, error format, versioning) that the
> generated spec doesn't narrate.

---

## 1. Overview

- **Base URL (local):** `http://localhost:8080`
- **API root:** all endpoints are under `/api`
- **Format:** JSON request and response bodies (`Content-Type: application/json`)
- **Auth:** stateless JWT (Bearer access token + refresh token) — see [Authentication](#3-authentication)
- **Dates:** ISO-8601 strings — `2026-07-15T12:00:00Z` for timestamps, `2026-07-15` for calendar dates
- **Money:** decimal `amount` + 3-letter `currency` code (default `USD`)
- **IDs:** UUID strings (except the legacy `/api/expenses` resource, which uses numeric ids)
- **CORS:** the Vite dev origins (`http://localhost:5173`, `http://127.0.0.1:5173`) are allowed by default; override with `APP_CORS_ALLOWED_ORIGINS`.

---

## 2. Response conventions

### Standard envelope

The auth endpoints **and** all current resource endpoints
(`/api/categories`, `/api/subcategories`, `/api/transactions`) wrap their
payload in this envelope:

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
| `data`    | object \| array \| null | Payload on success; `null` on error        |
| `error`   | string \| null  | Human-readable error message on failure           |
| `message` | string \| null  | Optional info message (e.g. "Transaction deleted")|

### Legacy exception — `/api/expenses`

> ⚠️ The original `/api/expenses` controller predates this convention: it returns
> the entity/array **directly** (no envelope) and is **not** user-scoped or
> auth-protected. It is kept for backward compatibility; new work uses
> `/api/transactions`. Treat `/api/expenses` as deprecated.

---

## 3. Authentication

JWT-based, stateless. Two tokens are issued:

| Token           | Lifetime | Sent as                              | Used for                    |
|-----------------|----------|--------------------------------------|-----------------------------|
| `accessToken`   | 1 hour   | `Authorization: Bearer <accessToken>`| Every authenticated request |
| `refreshToken`  | 7 days   | request body to `/api/auth/refresh`  | Getting a fresh access token|

**Flow**

1. `POST /api/auth/register` or `POST /api/auth/login` → returns `{ user, accessToken, refreshToken }`.
2. Send `Authorization: Bearer <accessToken>` on protected calls.
3. On `401` (expired token), call `POST /api/auth/refresh` with the refresh token for a new pair.
4. `POST /api/auth/logout` is a client-side discard (tokens are stateless); returns 200 for a stable contract.

**Which endpoints require auth**

- ✅ **Protected & user-scoped:** `/api/auth/me`, and every `/api/categories`,
  `/api/subcategories`, `/api/transactions` call. The user id is taken from the
  Bearer token (`@CurrentUser`); a missing/invalid token returns **401**, and a
  user only ever sees or edits **their own** rows.
- ❌ **Unprotected (legacy):** `/api/expenses/*` — no token required.

Passwords are hashed with **BCrypt**; the hash is never returned. There is no
Spring Security filter chain yet — the `@CurrentUser` argument resolver validates
the Bearer token per request.

---

## 4. Error handling

All errors are rendered by `GlobalExceptionHandler` as the failure envelope:

```json
{ "success": false, "data": null, "error": "A sub-category is required for this category", "message": null }
```

| Status | When                                                            |
|--------|-----------------------------------------------------------------|
| `400`  | Bean-validation failure (returns the first field message), malformed JSON, or a broken business rule (e.g. missing/foreign sub-category) |
| `401`  | Bad credentials, or missing/invalid/expired token                |
| `403`  | Forbidden (`ApiException.forbidden`)                            |
| `404`  | Resource not found (or not owned by the caller)                 |
| `409`  | Conflict — e.g. email already registered                       |
| `500`  | Unhandled server error (generic message; details logged server-side, never leaked) |

---

## 5. Data model

All entities are per-user. Timestamps (`createdAt`, `updatedAt`) are ISO-8601.

### Category — top-level bucket

| Field       | Type    | Notes                                                    |
|-------------|---------|----------------------------------------------------------|
| `id`        | UUID    |                                                          |
| `name`      | string  | ≤ 100 chars; **unique per user**                         |
| `kind`      | enum    | `INCOME` \| `EXPENSE`                                    |
| `bucket`    | enum    | `INCOME` \| `NEEDS` \| `WANTS` \| `SAVINGS` \| `OTHER` (50/30/20 budgeting) |
| `icon`      | string? | optional                                                 |
| `color`     | string? | optional                                                 |
| `isSystem`  | boolean | seeded defaults vs. user-created                         |
| `sortOrder` | int     | display order                                            |

### SubCategory — belongs to a Category

| Field          | Type      | Notes                                        |
|----------------|-----------|----------------------------------------------|
| `id`           | UUID      |                                              |
| `categoryId`   | UUID      | parent category                              |
| `name`         | string    | ≤ 100 chars; **unique per category**         |
| `icon`,`color` | string?   | optional                                     |
| `monthlyLimit` | decimal?  | optional monthly cap; when set must be **> 0** |
| `isSystem`     | boolean   |                                              |

### Transaction — a single money movement

| Field              | Type    | Notes                                                  |
|--------------------|---------|--------------------------------------------------------|
| `id`               | UUID    |                                                        |
| `name`             | string  | ≤ 200 chars                                            |
| `amount`           | decimal | **> 0**                                                |
| `currency`         | string  | 3-letter code, upper-cased; default `USD`              |
| `categoryId`       | UUID    | required                                               |
| `subCategoryId`    | UUID?   | **required when the category has sub-categories**      |
| `type`             | enum    | `INCOME` \| `EXPENSE`                                  |
| `transactionDate`  | date    | ISO date                                               |
| `note`             | string? | ≤ 500 chars                                            |
| `sourceType`       | enum    | `MANUAL` \| `RECURRING` \| `INSTALLMENT` — see [below](#7-recurring--installment-behavior) |
| `installmentNo`    | int?    | which installment this row is (recurring/installment only) |

> The entity also carries `recurringRuleId` and `installmentPlanId` FK columns
> (not exposed in the DTO yet) reserved for the engines described below.

### Core entry rules (enforced server-side)

- The `categoryId` must belong to the caller.
- A `subCategoryId` is **required** when the chosen category has any
  sub-categories, is **rejected** when the category has none, and must belong to
  the selected category — otherwise `400`.
- `amount` must be `> 0`; `currency` is normalised to upper-case (default `USD`).

---

## 6. Endpoints

### Auth — `/api/auth`  *(enveloped)*

| Method | Path        | Auth | Body                          | Returns                |
|--------|-------------|------|-------------------------------|------------------------|
| POST   | `/register` | —    | `RegisterRequest`             | `AuthResponse` (201)   |
| POST   | `/login`    | —    | `LoginRequest`                | `AuthResponse` (200)   |
| GET    | `/me`       | ✅   | —                             | `UserDto` (200)        |
| POST   | `/refresh`  | —    | `{ "refreshToken": "..." }`   | `AuthResponse` (200)   |
| POST   | `/logout`   | —    | —                             | `null` + message (200) |

### Categories — `/api/categories`  *(enveloped, auth ✅)*

| Method | Path      | Body              | Returns              |
|--------|-----------|-------------------|----------------------|
| GET    | `/`       | —                 | `CategoryDto[]`      |
| POST   | `/`       | `CategoryRequest` | `CategoryDto` (201)  |
| PUT    | `/{id}`   | `CategoryRequest` | `CategoryDto`        |
| DELETE | `/{id}`   | —                 | `null` + message     |

`CategoryRequest`: `{ name, kind, bucket, icon?, color? }`

### SubCategories — `/api/subcategories`  *(enveloped, auth ✅)*

| Method | Path                      | Body                 | Returns             |
|--------|---------------------------|----------------------|---------------------|
| GET    | `/?categoryId={uuid}`     | —                    | `SubCategoryDto[]`  (optional filter) |
| POST   | `/`                       | `SubCategoryRequest` | `SubCategoryDto` (201) |
| PUT    | `/{id}`                   | `SubCategoryRequest` | `SubCategoryDto`    |
| DELETE | `/{id}`                   | —                    | `null` + message    |

`SubCategoryRequest`: `{ categoryId, name, icon?, color?, monthlyLimit? }`

### Transactions — `/api/transactions`  *(enveloped, auth ✅)*

| Method | Path      | Query / Body                              | Returns               |
|--------|-----------|-------------------------------------------|-----------------------|
| GET    | `/`       | filters: `categoryId`, `subCategoryId`, `type`, `from`, `to` (all optional) | `TransactionDto[]` (newest first) |
| POST   | `/`       | `TransactionRequest`                      | `TransactionDto` (201)|
| PUT    | `/{id}`   | `TransactionRequest`                      | `TransactionDto`      |
| DELETE | `/{id}`   | —                                         | `null` + message      |

`TransactionRequest`: `{ name, amount, currency?, categoryId, subCategoryId?, type, transactionDate, note? }`

Results are sorted by `transactionDate` then `createdAt`, descending. `from`/`to`
are inclusive ISO dates.

### Expenses — `/api/expenses`  *(legacy, raw payloads, no auth)*

Deprecated first-generation endpoint kept for backward compatibility. CRUD plus
`/category/{category}`, `/date-range?startDate&endDate`, `/categories`. Prefer
`/api/transactions` for new work.

---

## 7. Recurring & installment behavior

> **Status: data model shipped, generation engine NOT yet implemented.**

The transaction schema is deliberately future-proofed for two upcoming features,
but **no endpoints create recurring rules or installment plans yet, and nothing
generates future rows** — every transaction created today is `sourceType: MANUAL`.

| Concept          | Intended behavior (per the feature plan)                                             | Today |
|------------------|--------------------------------------------------------------------------------------|-------|
| **Recurring** (periodic) | A rule (e.g. "rent, monthly") **materializes** real `Transaction` rows on a schedule + catch-up at login, so the user never re-enters it. Rows carry `sourceType=RECURRING` + `recurringRuleId`. | Not implemented — no `RecurringRule` model/endpoint. |
| **Installment** (*taksitli*) | A fixed total split into **N equal monthly** charges from a start date (e.g. 12.000 ₺ ÷ 12 = 1.000 ₺ × 12). Rows carry `sourceType=INSTALLMENT`, `installmentPlanId`, `installmentNo`. | Not implemented — no `InstallmentPlan` model/endpoint. |

**What exists now (schema-only):**

- `SourceType` enum: `MANUAL` (default today), `RECURRING`, `INSTALLMENT`.
- `Transaction.recurringRuleId`, `Transaction.installmentPlanId`, `Transaction.installmentNo` columns.
- `sourceType` and `installmentNo` are exposed read-only in `TransactionDto`.

**What is missing (tracked as future epics):**

- `RecurringRule` / `InstallmentPlan` entities, their CRUD endpoints, and the
  scheduled/catch-up generation engine.

See [`plan/FEATURE_CATEGORIES_TRANSACTIONS.md`](../../plan/FEATURE_CATEGORIES_TRANSACTIONS.md)
for the full design (Epic 4 = recurring, Epic 5 = installments).

---

## 8. Example payloads

### Create a transaction

`POST /api/transactions`  ·  `Authorization: Bearer <accessToken>`

```json
{
  "name": "Weekly groceries",
  "amount": 42.50,
  "currency": "USD",
  "categoryId": "9b1f...needs-uuid",
  "subCategoryId": "3c7a...groceries-uuid",
  "type": "EXPENSE",
  "transactionDate": "2026-07-15",
  "note": "Migros"
}
```

**201 Created**

```json
{
  "success": true,
  "data": {
    "id": "b2d4...",
    "name": "Weekly groceries",
    "amount": 42.50,
    "currency": "USD",
    "categoryId": "9b1f...needs-uuid",
    "subCategoryId": "3c7a...groceries-uuid",
    "type": "EXPENSE",
    "transactionDate": "2026-07-15",
    "note": "Migros",
    "sourceType": "MANUAL",
    "installmentNo": null,
    "createdAt": "2026-07-15T12:00:00Z",
    "updatedAt": "2026-07-15T12:00:00Z"
  },
  "error": null,
  "message": null
}
```

### Create a sub-category with a monthly limit

`POST /api/subcategories`

```json
{ "categoryId": "9b1f...needs-uuid", "name": "Groceries", "monthlyLimit": 600.00 }
```

### Validation error (missing required sub-category)

**400 Bad Request**

```json
{ "success": false, "data": null, "error": "A sub-category is required for this category", "message": null }
```

---

## 9. Common scenarios

**Add an expense under Needs → Groceries**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@example.com","password":"correcthorse"}' | jq -r '.data.accessToken')

# List categories to get the ids
curl -s http://localhost:8080/api/categories -H "Authorization: Bearer $TOKEN"

# Create the transaction
curl -s -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Groceries","amount":42.5,"categoryId":"<needs>","subCategoryId":"<groceries>","type":"EXPENSE","transactionDate":"2026-07-15"}'
```

**Filter this month's expenses in one category**

```bash
curl -s "http://localhost:8080/api/transactions?type=EXPENSE&categoryId=<uuid>&from=2026-07-01&to=2026-07-31" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Interactive docs & tooling

springdoc-openapi generates the spec from the controllers, so per-endpoint detail
never drifts from the code.

| Resource        | URL (local)                             |
|-----------------|-----------------------------------------|
| Swagger UI      | `http://localhost:8080/swagger-ui.html` |
| OpenAPI 3 spec  | `http://localhost:8080/v3/api-docs`     |

- **Swagger UI:** click **Authorize**, paste an `accessToken` (from `/api/auth/login`), and try any endpoint in-browser.
- **Postman:** *Import → Link →* `http://localhost:8080/v3/api-docs`.
- Metadata + the JWT scheme live in [`OpenApiConfig.java`](../../backend/src/main/java/com/expensetracker/config/OpenApiConfig.java).

---

## Where docs live

```
docs/api/README.md                       ← this file: conventions, data model, endpoint overview
backend/.../config/OpenApiConfig.java    ← OpenAPI metadata + JWT scheme
/v3/api-docs, /swagger-ui.html           ← generated per-endpoint reference (run the backend)
plan/FEATURE_CATEGORIES_TRANSACTIONS.md  ← feature design (recurring/installment epics)
```

Keep **conventions & data model** here; let **per-endpoint request/response
detail** come from the generated spec. When you add or change an endpoint the spec
updates automatically — touch this file only when a *convention, the data model,
or feature status* changes.
