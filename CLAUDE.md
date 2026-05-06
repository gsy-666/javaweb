# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo layout

- `backend/`: Spring Boot 3 (Java 17) API server.
- `web/`: Vue 3 + Vite + Ant Design Vue frontend (single app that renders different “ends” by role/routes).

## Common commands

### Frontend (web/)

Run the web dev server:

- `cd web && npm run dev`

Build (typecheck + bundle):

- `cd web && npm run build`

Preview production build:

- `cd web && npm run preview`

Run frontend + backend together (Windows-specific scripts in `web/package.json`):

- `cd web && npm run dev:backend` (starts backend on port 8082)
- `cd web && npm run dev:all` (starts backend in a new terminal + frontend dev server)

### Backend (backend/)

Run backend:

- `cd backend && mvn spring-boot:run`

Run backend without tests (matches existing frontend script):

- `cd backend && mvn -DskipTests spring-boot:run`

Run tests:

- `cd backend && mvn test`

Run a single test:

- `cd backend && mvn -Dtest=TestClassName test`

## High-level architecture

### Backend: Spring Boot API + Redis token auth + MyBatis-Plus

Entry point:
- `backend/src/main/java/com/example/repair/RepairApplication.java` uses `@MapperScan("com.example.repair.modules.*.mapper")` and `@EnableScheduling`.

Module layout:
- Feature code is under `backend/src/main/java/com/example/repair/modules/*/`.
  - Typical structure per module: `controller/`, `service/`, `mapper/`, `entity/`, `dto/`, `job/`.

Persistence:
- MyBatis-Plus is used for data access (`*Mapper` interfaces).
- Database schema and seed data are managed by Flyway migrations in:
  - `backend/src/main/resources/db/migration/`

Auth/session model:
- The frontend sends `Authorization: Bearer <token>`.
- The backend stores token sessions in Redis and loads the current principal in a servlet filter:
  - `backend/src/main/java/com/example/repair/common/security/UserContextFilter.java`
- Auth endpoints live under:
  - `backend/src/main/java/com/example/repair/modules/auth/controller/AuthController.java`

Key runtime config:
- `backend/src/main/resources/application.yml`:
  - server port defaults to `8082`
  - MySQL connection via `spring.datasource.*`
  - Redis via `spring.data.redis.*`
  - Flyway enabled
  - app-level settings under `app.*` (upload dir, dispatch weights, SLA timeouts, Amap reverse geocode)

### Frontend: Vue 3 SPA, three “ends” via role + routes

The frontend is a single Vue app that renders different “ends” (admin/worker/user) based on the authenticated role.

Routing:
- `web/src/router.ts` defines role-gated routes using `meta.roles`.
- A global guard redirects unauthenticated users to `/login` and redirects users to their allowed home if they hit a route not permitted for their role.

Session state:
- `web/src/user.ts` stores `{ userId, role, account, token }` in `localStorage` and exposes refs.
- `web/src/api.ts` attaches the Bearer token to axios requests and auto-logs out on 401/403.

API base URL:
- `web/src/api.ts` defaults API base URL to `http(s)://<host>:8082` unless `VITE_API_BASE_URL` is set.

“Situation” (态势) feature:
- `web/src/views/SituationView.vue` is the cross-role situation page.
- Supporting state/logic lives in `web/src/situation/*` (focus state, normalization, SLA synthesis, etc.).

## UI direction (project-specific)

This repo is being actively redesigned to look like a production “three-end” system:

- Admin (管理端): dark, high-contrast, “tech” dashboard style.
- Worker (工人端): light, clean, task-oriented console style.
- User/Repair (报修端): warm light palette, friendly card-based flows.

Implementation note:
- Even though the product is “three ends”, it is currently implemented as one SPA (`web/`) with role-based layouts/themes.


$env:AMAP_KEY="849c6c557123db917b6d95b4cf2a7921"
mvn -DskipTests spring-boot:run
