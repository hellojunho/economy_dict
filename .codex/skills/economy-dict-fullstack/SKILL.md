---
name: economy-dict-fullstack
description: Full-stack feature work for the economy_dict repository. Use when changing Spring Boot APIs, React/Vite pages, PostgreSQL-backed flows, JWT auth behavior, or Docker/Nginx wiring in this project.
---

# Economy Dict Fullstack

Use repository conventions instead of inventing new ones.

## Inspect first
- Read `/Users/chojunho/project/economy_dict/backend/src/main/resources/application.properties` for active ports and app settings.
- Read `/Users/chojunho/project/economy_dict/docker-compose.yml` for service names, mounted paths, and exposed ports.
- Read `/Users/chojunho/project/economy_dict/frontend/src/App.tsx` before changing routes or navigation.

## Backend rules
- Keep API style REST-first.
- Keep auth endpoints at `/api/signup`, `/api/token`, `/api/logout` unless the user explicitly requests a new contract.
- Return structured JSON errors; do not fall back to HTML or plain text.
- Preserve admin vs general separation through role-based authorization.

## Frontend rules
- Keep routes separated by purpose.
- Auth pages must render one form per page.
- Reuse existing API client and Zustand stores before introducing new fetch wrappers.
- Keep text concise and action labels explicit.

## Data and infra rules
- If a change affects upload, logging, or persistence, verify Docker volume paths.
- If a change affects auth, verify both frontend route handling and backend permit rules.
- If a change affects a table-driven screen, keep create, update, and delete all wired.

## Validation
- For frontend-only changes, run `npm run build` in `/Users/chojunho/project/economy_dict/frontend`.
- For backend or integrated changes, prefer Docker build and targeted endpoint checks.
