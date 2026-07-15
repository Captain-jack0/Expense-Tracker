# Feature Plan — Categorized Transactions, Budgets, Recurring & Installments

> Status: **Draft for review** · Owner: Meryem · Created: 2026-07-14
> Scope: Extend the Expense Tracker beyond the existing balance overview with a
> full category/sub-category system, per-sub-category spending limits, a rich
> transaction entry flow, and support for recurring (periodic) and installment
> (*taksitli*) payments so future months are generated automatically.

---

## 1. Goal (from request)

When a user signs up and starts entering money movements, they should be able to:

1. Pick a **top-level category**: Income, Needs, Wants, Savings (Balance overview already exists).
2. Enter a **transaction name** and **amount**.
3. If the chosen category has **sub-categories** (e.g. Needs → *Groceries*), pick one.
4. Sub-categories may carry an optional **spending limit** (monthly cap).
5. Enter the **date** the money was spent.
6. Mark the entry as **recurring (periodic)** *or* **installment (*taksitli*)** so it is
   **created automatically in the following months** — the user must not have to
   re-enter it every month.
7. See it all reflected in the **overview** against budgeting **rules for finance**.

---

## 2. Assumptions & open questions

These drive the task breakdown. Please confirm or correct — cheap to change now.

| # | Assumption | If wrong… |
|---|------------|-----------|
| A1 | **"Rules for finance" = 50/30/20 budget rule**: target % for Needs (50) / Wants (30) / Savings (20), configurable per user. | Replace Epic 6 with the intended rule engine. |
| A2 | Top-level categories are **system-seeded** (Income, Needs, Wants, Savings, Other) but the user can **add their own** too. | If fixed-only, drop the category CRUD FE/BE tasks. |
| A3 | **"Expenses"** in the request is the umbrella term for Needs+Wants+Other, not a separate sibling bucket. Modeled as an `Other/General` expense category. | Add a distinct top-level "Expenses" category. |
| A4 | Sub-category limits are **monthly** and reset each calendar month. | Support weekly/custom windows. |
| A5 | **Installment** = a fixed total split into *N equal monthly* charges starting on a date (e.g. 12.000 ₺ / 12 = 1.000 ₺ x 12). | Support variable amounts / other intervals. |
| A6 | **Recurring** occurrences are **materialized** (real Transaction rows generated on a schedule + catch-up at login), not computed on the fly. | Switch to virtual/derived occurrences. |
| A7 | Currency, auth and the balance overview from Phase 1 stay as-is; this feature plugs into them. | — |

---

## 3. Proposed data model

```
Category            (top-level bucket)
  id, userId(null = system), name, kind(INCOME|EXPENSE),
  bucket(NEEDS|WANTS|SAVINGS|INCOME|OTHER), icon, color, isSystem, sortOrder

SubCategory         (belongs to a Category)
  id, categoryId, userId, name, icon, color,
  monthlyLimit(nullable, >0), isSystem

Transaction         (extends the existing entity)
  id, userId, name, amount, currency,
  categoryId, subCategoryId(nullable), type(INCOME|EXPENSE),
  transactionDate, note,
  sourceType(MANUAL|RECURRING|INSTALLMENT),
  recurringRuleId(nullable), installmentPlanId(nullable), installmentNo(nullable)

RecurringRule
  id, userId, name, amount, categoryId, subCategoryId,
  frequency(WEEKLY|MONTHLY|YEARLY), interval(int>=1),
  startDate, endDate(nullable), nextRunDate, active

InstallmentPlan
  id, userId, name, totalAmount, installmentCount,
  categoryId, subCategoryId, startDate, perInstallmentAmount, active

BudgetRule          ("rules for finance", 50/30/20)
  id, userId, needsPct, wantsPct, savingsPct   (must sum to 100)
```

**API surface (Spring Boot, all under `/api`, all wrapped in the existing
`ApiResponse<T>` envelope):**

```
GET/POST/PUT/DELETE  /categories                 /categories/{id}
GET/POST/PUT/DELETE  /subcategories              /subcategories/{id}
GET                  /subcategories/{id}/spending?month=YYYY-MM
GET/POST/PUT/DELETE  /transactions               /transactions/{id}
GET                  /transactions?categoryId=&subCategoryId=&from=&to=&type=
GET/POST/PUT/DELETE  /recurring-rules            /recurring-rules/{id}
POST                 /recurring-rules/run        (materialize due occurrences)
GET/POST/DELETE      /installment-plans          /installment-plans/{id}
GET/PUT              /budget-rule
GET                  /overview?month=YYYY-MM      (buckets vs target, limits, upcoming)
```

---

## 4. Epics & tasks

Priority: **P1** = core / do first · **P2** = important · **P3** = polish.
IDs: `ET-BE-xx` (backend, Spring Boot) · `ET-FE-xx` (frontend, React/TS).

### Epic 1 — Category & sub-category foundation

- **ET-BE-01 · Category & SubCategory entities + persistence** — P1
  Create `Category`, `SubCategory` JPA entities, repositories, DB schema.
  *AC:* tables created on H2/MySQL; unique (userId,name) per level; cascade rules defined; unit test for repository CRUD.
- **ET-BE-02 · Seed default categories & sub-categories** — P1 · dep: ET-BE-01
  Seed on first run: Income; Needs (Rent, Groceries, Utilities, Transport, Health); Wants (Dining, Entertainment, Shopping); Savings (Emergency fund, Investment); Other.
  *AC:* new user gets system categories; seeding is idempotent.
- **ET-BE-03 · Category CRUD API** — P1 · dep: ET-BE-01
  `GET/POST/PUT/DELETE /categories`, scoped to current user + system read-only.
  *AC:* user cannot edit/delete system categories; validation errors return `ApiResponse.fail`; 80% test coverage on service.
- **ET-BE-04 · SubCategory CRUD API** — P1 · dep: ET-BE-01
  `GET/POST/PUT/DELETE /subcategories`, filter by `categoryId`.
  *AC:* sub-category must belong to a category owned/visible to user; tests.
- **ET-FE-01 · Category/SubCategory types + API client** — P1
  Extend `types/index.ts` and `services/api.ts` (`categoriesApi` already stubbed; add `subCategoriesApi`).
  *AC:* typed client methods; React Query hooks; no `any`.
- **ET-FE-02 · Category management screen** — P2 · dep: ET-FE-01, ET-BE-03
  List categories grouped by bucket; add/edit/delete user categories; icon+color picker.
  *AC:* optimistic updates; system categories shown read-only.
- **ET-FE-03 · Sub-category management (within a category)** — P2 · dep: ET-FE-02, ET-BE-04
  Expand a category to manage its sub-categories.
  *AC:* create/edit/delete; empty-state guidance.

### Epic 2 — Sub-category spending limits (budgets)

- **ET-BE-05 · `monthlyLimit` on SubCategory + validation** — P1 · dep: ET-BE-01
  Add nullable positive `monthlyLimit`; expose in sub-category API.
  *AC:* reject `<= 0`; null = no limit; migration safe.
- **ET-BE-06 · Spending-vs-limit endpoint** — P2 · dep: ET-BE-05, ET-BE-08
  `GET /subcategories/{id}/spending?month=YYYY-MM` → {spent, limit, remaining, pct, overLimit}.
  *AC:* sums only that user's expense transactions in the month; tested with boundary dates.
- **ET-FE-04 · Limit field in sub-category form** — P2 · dep: ET-FE-03, ET-BE-05
  Optional "Monthly limit" input.
  *AC:* zod validation (>0 or empty); persists.
- **ET-FE-05 · Limit progress + over-limit warning** — P2 · dep: ET-FE-04, ET-BE-06
  Progress bar spent/limit; red state + warning when exceeded.
  *AC:* accessible colors; shows "no limit set" when null.

### Epic 3 — Transaction entry with category & sub-category

- **ET-BE-07 · Extend Transaction entity** — P1 · dep: ET-BE-01
  Add `name`, `subCategoryId`, `sourceType`, `recurringRuleId`, `installmentPlanId`, `installmentNo`; keep `amount`, `categoryId`, `transactionDate`, `type`.
  *AC:* migration; existing rows unaffected; entity tests.
- **ET-BE-08 · Create/Update transaction API + rules** — P1 · dep: ET-BE-07, ET-BE-04
  Validate: name & amount required; category required; **sub-category required when the category has sub-categories**; sub-category must belong to the category; ownership enforced.
  *AC:* 400 with clear message on rule violations; happy-path + rule tests.
- **ET-BE-09 · List/filter transactions** — P1 · dep: ET-BE-08
  `GET /transactions` with filters (categoryId, subCategoryId, from, to, type) + pagination.
  *AC:* filters composable; paginated envelope; tests.
- **ET-FE-06 · Transaction entry form** — P1 · dep: ET-FE-01, ET-BE-08
  Fields: name, amount, category select, **dependent** sub-category select, date picker.
  *AC:* zod schema; server errors surfaced; disabled submit while invalid.
- **ET-FE-07 · Dependent sub-category dropdown** — P1 · dep: ET-FE-06
  Sub-category options load from the selected category; required only when options exist.
  *AC:* clears sub-category when category changes; loading/empty handled.
- **ET-FE-08 · Transaction list + edit/delete** — P2 · dep: ET-FE-06, ET-BE-09
  Filterable list; edit reopens the form; delete with confirm.
  *AC:* optimistic update; filter by category/sub-category/date.

### Epic 4 — Recurring (periodic) expenses

- **ET-BE-10 · RecurringRule model + API** — P1 · dep: ET-BE-07
  Entity + `GET/POST/PUT/DELETE /recurring-rules` (frequency, interval, start/end, template amount/category/sub-category).
  *AC:* validation (interval>=1, endDate>startDate); tests.
- **ET-BE-11 · Occurrence generation (idempotent)** — P1 · dep: ET-BE-10
  Given "now", generate all due Transactions from active rules up to today; never double-generate (track `nextRunDate` / occurrence key).
  *AC:* running twice creates each occurrence once; unit tests over month boundaries & leap cases.
- **ET-BE-12 · Scheduler + login catch-up** — P2 · dep: ET-BE-11
  `@Scheduled` daily job **and** a catch-up call on login/app-load (`POST /recurring-rules/run`) so a user who was away still gets past months filled.
  *AC:* documented trigger; safe to call repeatedly.
- **ET-FE-09 · "Repeat" controls in the form** — P1 · dep: ET-FE-06, ET-BE-10
  Toggle → frequency (weekly/monthly/yearly), interval, optional end date.
  *AC:* preview text ("Every month until …"); validation.
- **ET-FE-10 · Recurring rules management** — P2 · dep: ET-FE-09, ET-BE-12
  List active rules, show next run, pause/stop, delete.
  *AC:* pausing stops future generation; past rows untouched.

### Epic 5 — Installment payments (*taksitli ödeme*)

- **ET-BE-13 · InstallmentPlan model + generation** — P1 · dep: ET-BE-07
  On create: split `totalAmount` into `installmentCount` equal monthly Transactions (rounding remainder into the last one), linked via `installmentPlanId` + `installmentNo`.
  *AC:* sum of installments == total (to the cent); N rows across N months; tests.
- **ET-BE-14 · Installment plan API** — P2 · dep: ET-BE-13
  `GET/POST/DELETE /installment-plans`; show paid vs remaining; delete cancels **future** unpaid installments only.
  *AC:* deleting keeps already-past charges; tests.
- **ET-FE-11 · Installment option in the form** — P1 · dep: ET-FE-06, ET-BE-13
  Inputs: total, count → live preview of per-month amount and schedule.
  *AC:* preview updates on change; validation (count>=2).
- **ET-FE-12 · Installment plan detail** — P3 · dep: ET-FE-11, ET-BE-14
  Paid/remaining, upcoming months, cancel remaining.
  *AC:* clear paid vs upcoming states.

### Epic 6 — Budget rules (50/30/20) & overview integration

- **ET-BE-15 · BudgetRule config API** — P2
  `GET/PUT /budget-rule` (needsPct+wantsPct+savingsPct == 100), default 50/30/20.
  *AC:* rejects sums != 100; per-user; tests.
- **ET-BE-16 · Overview/summary endpoint** — P1 · dep: ET-BE-09, ET-BE-15
  `GET /overview?month=` → income, spend per bucket, actual-vs-target, savings rate, sub-category limit statuses, upcoming recurring/installments.
  *AC:* one call powers the dashboard; tested aggregation.
- **ET-FE-13 · "Rules for finance" settings** — P3 · dep: ET-BE-15
  Edit 50/30/20 targets with a sum=100 guard.
  *AC:* validation; persists.
- **ET-FE-14 · Overview widgets** — P2 · dep: ET-FE-01, ET-BE-16
  Bucket breakdown vs target, sub-category limit statuses, upcoming recurring/installment list on the dashboard.
  *AC:* uses `/overview`; animated (Recharts/Framer already in stack).

### Epic 7 — Cross-cutting quality

- **ET-BE-17 · Backend tests to 80%** — P2
  Service + controller + repository tests across the new modules (JUnit 5).
  *AC:* JaCoCo ratio raised toward 0.80 per `pom.xml` plan.
- **ET-FE-15 · Frontend tests** — P2
  Vitest for form/validation + Playwright E2E for "add categorized transaction" and "create recurring".
  *AC:* critical flows green in CI.
- **ET-DOC-01 · Update README & API docs** — P3
  Document new endpoints, data model, and the recurring/installment behavior.
  *AC:* README reflects the shipped feature.

---

## 5. Suggested delivery order

1. **Foundation:** ET-BE-01 → 02 → 07 → 03/04, ET-FE-01.
2. **Core entry:** ET-BE-08 → 09, ET-FE-06 → 07 → 08.
3. **Limits:** ET-BE-05 → 06, ET-FE-04 → 05.
4. **Recurring:** ET-BE-10 → 11 → 12, ET-FE-09 → 10.
5. **Installments:** ET-BE-13 → 14, ET-FE-11 → 12.
6. **Rules & overview:** ET-BE-15 → 16, ET-FE-13 → 14.
7. **Quality:** ET-BE-17, ET-FE-15, ET-DOC-01.

---

## 6. Export to task-manager

These tasks are also exported for the **task-manager** app (`Projeler/task-manager`) as
an importable file + loader script — see
[`task-manager/seeds/README.md`](../../task-manager/seeds/README.md).
