# CivicTrack

A compact Spring Boot prototype for reporting civic issues, routing them to departments, tracking accountable status changes, and waiting for citizen verification before final closure.

## Run locally

1. Copy `.env.example` to `.env` and change `JWT_SECRET` before use.
2. Start the PostgreSQL container with `docker compose up -d postgres`.
3. Run `mvn spring-boot:run` from this directory.
4. Open `http://localhost:8080`.

The Compose service runs only PostgreSQL; Spring Boot remains a normal local process. The named Docker volume keeps database data between container restarts. Migrations run automatically through Flyway. JobRunr stores its operational tables in the same PostgreSQL database and checks SLA deadlines every 15 minutes. Its dashboard is available at `/dashboard` while running.

To stop the database while retaining its data, run `docker compose down`. To remove the database volume as well, run `docker compose down -v`.

## Roles

Public registration creates a `CITIZEN`. `OFFICIAL` and `ADMIN` users are deliberately not self-service: create them directly in the prototype database (with a BCrypt password) or through a database administration tool. Admin users can create departments, map categories, and assign an existing official. Backend endpoint restrictions and ownership checks enforce the role model.

## Lifecycle

`SUBMITTED → ASSIGNED → IN_PROGRESS → RESOLVED → VERIFIED`; a citizen may transition `RESOLVED → REOPENED`, which returns the item to `IN_PROGRESS`. Every transition writes a history record. An official's resolution never closes the case on its own.

## Optional AI

By default the app targets a local Ollama instance at `http://localhost:11434/api/generate` using `llama3.1:8b`. Install and start Ollama, then run `ollama pull llama3.1:8b`. Set `LLM_API_URL` or `LLM_MODEL` to override those defaults; `LLM_API_KEY` is intentionally blank for local Ollama. The JobRunr worker requests non-streaming JSON and saves its untrusted suggestion without overwriting citizen text. If Ollama is unavailable, classification is marked `FAILED` and core functionality continues normally.

## Not implemented intentionally

S3 presigned evidence upload is not included: it is optional and was deferred until core workflow is proven. Notification delivery is stored in-app; external email delivery is also intentionally out of scope for this prototype.
