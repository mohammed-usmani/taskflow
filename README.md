# TaskFlow

A small team task tracker: a Spring Boot + Kotlin REST API and a React + TypeScript frontend.
Users register, create projects, and manage tasks on a three-column board with filters, sorting,
a status summary and overdue highlighting.

## Quick start (Docker)

```bash
docker compose up --build
```

| What | URL |
|---|---|
| App | http://localhost:3000 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |

**Demo login:** `demo@taskflow.dev` / `demo1234`. The demo user has one project with five tasks
(one overdue) and a teammate, Sam, to assign tasks to. Seeding runs once and can be turned off
with `SEED_DEMO_DATA=false`.

## Run locally (for development)

Requirements: JDK 21, Node 24+, Docker (for PostgreSQL).

```bash
docker compose up -d postgres        # PostgreSQL 17 on localhost:5434

cd backend && ./gradlew bootRun      # API on http://localhost:8080 (Flyway creates the schema)

cd frontend && npm install && npm run dev   # app on http://localhost:5173
```

The frontend calls relative `/api/...` URLs. In development Vite proxies them to `localhost:8080`;
in Docker, nginx does the same. The browser only ever talks to one origin.

### Configuration

Everything comes from environment variables, with local defaults in `application.yml`. No secrets
are in the code.

| Variable | Default (local only) | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5434/taskflow`, `taskflow`, `taskflow` | database |
| `JWT_SECRET` | a dev-only value | HMAC key for signing tokens (**set it** outside local use) |
| `JWT_EXPIRATION_MINUTES` | `60` | token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | only needed if the frontend is served from another origin |
| `SEED_DEMO_DATA` | `true` | create the demo user and project |

## Tests

```bash
cd backend && ./gradlew test    # 36 tests: JUnit 5 + MockK unit tests, @SpringBootTest integration tests
cd frontend && npm test         # 20 tests: Jest + React Testing Library
```

- **Backend unit tests (MockK):** `AuthServiceTest`, `ProjectServiceTest`, `TaskServiceTest`,
  `JwtServiceTest`. They cover the service rules: ownership, defaults, trimming, unknown assignee,
  and identical errors for an unknown email and a wrong password.
- **Backend integration tests** run against the real PostgreSQL (start it first), each in a rolled-back
  transaction:
  - `AuthIntegrationTest`, `ProjectIntegrationTest`, `TaskApiIntegrationTest`: the HTTP layer, including
    status codes, field-level validation errors, the JSON shape, and 401s and 404s for other users' data;
  - `TaskQueryIntegrationTest`: the real SQL for filters, sorting, pagination and the summary.
- **Frontend component tests** render the real routes and providers with only the API layer mocked:
  - login and protected routes;
  - projects list and create;
  - the board (columns, overdue, optimistic status change and rollback, keyboard drag-and-drop, filters in the URL);
  - the task form (validation, create/edit payloads, delete confirmation).

CI (GitHub Actions) runs both suites on pushes to `main` and on pull requests. The backend job starts a PostgreSQL service container.

## Architecture

```
backend/src/main/kotlin/com/taskflow/
  auth/      register/login, JwtService, JwtAuthFilter
  user/      User entity, /api/users
  project/   entity, repository, service, controller, DTOs
  task/      entity, repository, service, controller, DTOs, TaskQuery (filters + sorting)
  common/    ErrorResponse, domain exceptions, GlobalExceptionHandler
  config/    SecurityConfig, OpenAPI, demo data seeder
frontend/src/
  api/         axios client + interceptors, one module per resource, shared types, query keys
  auth/        AuthContext, ProtectedRoute / GuestRoute
  components/  Button, Modal, Field, TaskCard, TaskFormModal, TaskFilters, SummaryBar, states
  pages/       Login, Register, Projects, ProjectBoard (+ useBoard hook, BoardColumns with drag-and-drop)
```

**Backend:** `controller → service → repository`, packaged by feature.
- **Controllers** are thin: they read input, validate it (`@Valid`) and choose the status code.
- **Services** hold the rules and the transaction boundaries.
- **Repositories** are Spring Data JPA.
- **Entities never leave the service layer;** every response is a DTO.
- **The schema is owned by Flyway** (`V1__initial.sql`), and Hibernate only validates it (`ddl-auto: validate`).

**Security:**
- Stateless JWT authentication. `JwtAuthFilter` reads the Bearer token and puts the user id in the
  security context; `SecurityConfig` requires authentication everywhere except register, login, docs
  and health.
- Passwords are hashed with BCrypt.
- **Ownership is enforced in one place:** `ProjectService.requireOwned` for project URLs, and a single
  `findByIdAndProjectOwnerId` query for task URLs.
- Someone else's project or task returns **404, not 403**, so the API never confirms that it exists.

**Errors:** every error has the same JSON shape from `GlobalExceptionHandler`:
`status, error, message, path, timestamp, fieldErrors[]`.
- Validation failures return 400 with one entry per field.
- Unexpected exceptions are logged and returned as a generic 500, with no stack traces.
- 401s from the security layer use the same shape.

**Frontend:**
- **API layer:** one axios instance, with interceptors that attach the token and turn any 401 into a clean
  logout ("Your session expired"). Components never call axios directly.
- **Server state:** React Query.
- **Auth state:** a small Context.
- **Forms:** react-hook-form + zod, with the same rules as the backend.
- **Filters and sort** live in the URL, so a reload or a shared link keeps the view.
- **Moving tasks:** drag a card by its handle (⠿) to another column, or use its status dropdown.
  Both call the same mutation. Drag-and-drop uses `@dnd-kit` and works with mouse, touch and keyboard
  (focus the handle, Space, arrow keys to change column, Space to drop), with screen-reader announcements.

## Design patterns

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `task/TaskQuery.kt`, `TaskSort` | Each sort option is an object that builds its own `ORDER BY`. The service picks one by name, with no `if/when` on sort fields. Priority sorts by rank (LOW < MEDIUM < HIGH), not alphabetically. Adding a sort is one enum entry. |
| **Specification** (composable filters) | `TaskFilters` in `TaskQuery.kt` | Each filter is a small predicate. The service combines only the ones the client sent, instead of a `findBy…` method for every combination. |
| **Factory method** | `TaskResponse.from`, `ProjectResponse.from`, `UserResponse.from` | One place maps each entity to its DTO (and computes `overdue`), so no entity leaks into a response. |
| **Repository** | Spring Data interfaces | Persistence behind interfaces; services stay testable with MockK. |

## State management choice

**React Query for server data, Context for auth.**
- **Almost everything on screen is server data:** projects, tasks, the summary and users. React Query
  gives every call loading, error and retry states, caching, and invalidation after a change.
- **Status changes (drag-and-drop or dropdown) use an optimistic update:** the card moves at once, rolls
  back if the server refuses, and refetches either way.
- **Redux Toolkit would mean re-implementing that by hand** for little gain at this size.
- **The only client-only state is "who is logged in",** which a small Context covers.

## Assumptions

- **Any registered user can be assigned a task.** The brief defines no teams or project members.
  Visibility stays owner-only: an assignee who doesn't own the project can't see it.
- **"dueDate not in the past" applies on create only,** as the brief states. An existing overdue task
  must stay editable (for example, to mark it done).
- **Overdue** means a due date before today and a status that isn't DONE. The server computes it, so the
  board and the summary always agree.
- **The summary counts the whole project,** regardless of the board's filters (the UI says so).
- **Dates** use the server's calendar date. Timestamps are stored as `TIMESTAMPTZ` in UTC.

## Trade-offs and next steps

- **JWT in localStorage:** simple and stateless, but readable by any script on the page if there were an
  XSS bug. Next step: an httpOnly cookie plus CSRF protection, or short-lived access tokens with refresh tokens.
- **Tokens can't be revoked before they expire** (60 minutes). Next: refresh tokens with a server-side denylist.
- **The board loads up to 100 tasks** (the API's page cap) and shows a notice beyond that. Next: per-column
  pagination or infinite scroll.
- **Drag-and-drop moves a task between columns, not within one.** The API has no manual ordering; cards
  follow the chosen sort. Next: a `position` column, if hand-ordering within a column is wanted.
- **`GET /api/users` isn't paginated.** Fine for small teams. Next: search and paging, or project members
  (which would also extend visibility to assignees).
- **Activity log (bonus) not built.** Plan: a `task_activity` table written in the same transaction as each
  status change, listed on the task.

## Bonus items

| Item | Status |
|---|---|
| Docker Compose (backend + frontend + PostgreSQL, one command) | ✅ |
| OpenAPI / Swagger UI | ✅ `/swagger-ui.html`, with an Authorize button for the JWT |
| Flyway migrations | ✅ |
| GitHub Actions (build + test both apps) | ✅ |
| Activity log | ❌ planned, see above |
