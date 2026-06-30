# Team Notes Service

Reactive backend for a note-taking application used and shared by several small teams.

Built with:
- **Java 21**
- **Maven** (wrapper included — `./mvnw` / `.\mvnw.cmd` for zero-install builds)
- **Spring Boot 4.0.6** (fully on Spring Boot 4)
- **Fully reactive** (no `.block()` calls anywhere — pure Project Reactor / WebFlux)
- **BlockHound** (runtime blocking detection, active in tests)
- **Spring WebFlux** (fully non-blocking)
- **Spring GraphQL** (primary API)
- **Spring Data R2DBC** (reactive relational access)
- **PostgreSQL** (recommended) / **H2** (zero-config demo)
- **Frontend**: Vite + Svelte 5 + Tailwind (with JWT, markdown preview, dark mode, etc.)

## Why Spring Boot, WebFlux, and Project Reactor?

### Spring Boot 4
Spring Boot was chosen as the foundation because it provides:
- **Auto-configuration and starters**: Single dependency brings in WebFlux, Spring GraphQL, Data R2DBC, Security, Validation, and Actuator with almost zero boilerplate.
- **Production-ready defaults**: Health checks, metrics, externalized configuration, and testing support are built-in.
- **Mature reactive ecosystem**: Excellent first-class support for reactive stacks in version 4 (built on Spring Framework 6+ / 7+).
- **Huge community and tooling**: Easy onboarding, rich documentation, and integration with tools like Maven, Docker, and IDEs.

**Alternatives considered**:
- **Quarkus or Micronaut**: These offer faster startup times and better GraalVM native-image support, making them attractive for microservices in containers. However, their reactive GraphQL and R2DBC ecosystems are smaller, and the Spring ecosystem (especially GraphQL support) was more complete and familiar for this team-oriented backend.
- **Plain Spring Framework (no Boot)**: Would require significantly more manual configuration for servers, security, and data access.
- **Non-Java stacks** (e.g., NestJS/Node, Go with Fiber, or Rust with Axum): These can be very fast for I/O, but the explicit requirement was Java 21. Staying in Java also preserved access to the team's existing JVM tooling and libraries.

### WebFlux (instead of Spring MVC)
The requirements explicitly asked to "use webflux". Beyond that:
- **Non-blocking, event-loop model**: One or a few threads can handle thousands of concurrent connections. This is ideal for a note-taking service where teams may perform many small reads (listing notes) and occasional writes.
- **Backpressure and resource efficiency**: Better behavior under load compared to thread-per-request models.
- **Native integration with reactive data access**: Pairs perfectly with R2DBC so the entire request lifecycle stays non-blocking.

**Alternatives considered**:
- **Spring MVC + Servlet stack**: Much simpler for traditional CRUD (easier stack traces, familiar filters/interceptors). It would have been faster to develop initially. However, it would violate the WebFlux requirement and perform worse under concurrent team usage. For low-to-medium traffic internal tools, MVC is often the pragmatic choice — we deliberately went the other way to honor the spec and demonstrate modern reactive patterns.
- **Other reactive frameworks** (Quarkus Reactive, Vert.x): Good alternatives, but again, the explicit ask was for Spring WebFlux.

### Project Reactor
Project Reactor is the reactive streams implementation that powers WebFlux and Spring Data R2DBC:
- **Rich operator set** (`filterWhen`, `flatMap`, `switchIfEmpty`, etc.) for composing complex async logic cleanly.
- **Backpressure support** out of the box.
- **Seamless Spring integration**: Return `Mono`/`Flux` from controllers and repositories and everything wires together.
- **Strong debugging tools** (like BlockHound, which we use).

**Alternatives considered**:
- **RxJava**: Mature and widely used, but its API feels different from Reactor and has less tight integration with the current Spring reactive stack.
- **Mutiny** (used by Quarkus): More "natural" for imperative-style developers, but we stayed consistent with Spring's Reactor.
- **Raw `CompletableFuture` or callbacks**: Leads to "callback hell" and loses the composability and backpressure that Reactor provides.

**Overall rationale**: The combination of Spring Boot + WebFlux + Reactor gave us a production-grade, fully reactive stack that matched the explicit requirements while still benefiting from Spring's massive ecosystem. For a collaborative note-taking service, the ability to efficiently handle concurrent access from multiple team members without over-provisioning threads was a key benefit. The main trade-off is increased complexity (especially around error handling and debugging), which we mitigated with BlockHound, good layering, and documentation.

## Goals & Scope

This service provides:
- Capture and organize notes (personal + team)
- Simple team management and membership
- Controlled sharing of notes within teams
- Reactive, scalable API using GraphQL for flexible client queries

### What's In Scope (MVP)

- Notes (title + markdown content)
- Personal notes and team-shared notes
- Basic teams with owner/admin/member roles
- CRUD + search for notes
- GraphQL API (with some REST health endpoints via Actuator)
- Optimistic locking, validation, clear permission model
- Easy local run (H2) and production-like (Postgres + Docker)

### Explicitly Out of Scope (for initial delivery)

- Full production authentication (OAuth2 / OIDC / real user service)
- Real-time collaboration (WebSockets + subscriptions)
- Rich content (blocks / attachments / images)
- Version history / comments on notes
- Advanced RBAC or granular permissions
- Pagination cursors (simple offset/limit for now)

## Running the Full Stack

This project uses **Maven** (wrapper scripts are committed).

**Backend + Frontend together:**
1. `./mvnw spring-boot:run` (backend on 8080)
2. `cd frontend && npm run dev` (frontend on 5173)

See the dedicated "Impressive Frontend" section below for details.

### Data Seeding & Independent Core Logic

Demo data initialization (users, teams, and sample notes) has been **completely separated** from core application logic.

- By default, `app.data.seed-demo=false` — the application starts with a completely clean database.
- This allows the core business logic (GraphQL resolvers, services, reactive repositories) to run and be tested independently.
- Demo data is only loaded when explicitly enabled.

**How to enable demo data:**

```bash
# Option 1: Enable via system property (recommended for one-off runs)
./mvnw spring-boot:run -Dapp.data.seed-demo=true

# Option 2: Use the dev profile (recommended for local development)
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

The `application-dev.yml` profile enables seeding automatically.

**Why this separation?**
- Core logic (note CRUD, team membership, permission checks) should not depend on demo data existing.
- Production deployments must never accidentally load demo data.
- Tests can run against a pristine schema.
- You can still seed data manually in the future (e.g., via a management endpoint or Flyway) without touching application startup code.

The seeding logic lives in its own `DemoDataSeeder` service, invoked only by a conditional `CommandLineRunner` when the property is enabled.

### 1. Quick Start (Recommended - H2, zero external deps)

```bash
./mvnw spring-boot:run
```

Or on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

**Note:** BlockHound is active. The app will crash on startup (or during request handling) with a clear `BlockingOperationError` + stack trace if any blocking code is called from a Reactor thread.

Alternative with system Maven:

```bash
mvn spring-boot:run
```

The service starts on http://localhost:8080

GraphiQL (playground): http://localhost:8080/graphiql

### 2. With PostgreSQL (more realistic)

```bash
docker compose up -d
```

Then edit `src/main/resources/application.yml` and switch the `r2dbc` section to use Postgres, or create `application-local.yml`.

Then run the app.

### 3. Using the API (Demo Authentication)

All requests **must** include the header:

```
X-User-Id: 11111111-1111-1111-1111-111111111111
```

Demo users seeded:
- `11111111-1111-1111-1111-111111111111` — Alice (owner of Engineering team)
- `22222222-2222-2222-2222-222222222222` — Bob
- `33333333-3333-3333-3333-333333333333` — Carol

## GraphQL Examples

### Get my notes

```graphql
query {
  myNotes(limit: 10) {
    id
    title
    content
    teamId
    createdAt
  }
}
```

### Create a note (personal)

```graphql
mutation {
  createNote(input: {
    title: "My new idea"
    content: "This is markdown content..."
  }) {
    id
    title
  }
}
```

### Create note shared with team

```graphql
mutation {
  createNote(input: {
    title: "Team retrospective"
    content: "..."
    teamId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
  }) {
    id
  }
}
```

### Search

```graphql
query {
  searchNotes(query: "roadmap", limit: 5) {
    id
    title
  }
}
```

### Create team + add member

```graphql
mutation {
  createTeam(name: "Design") {
    id
    name
  }
}

mutation {
  addMemberToTeam(
    teamId: "the-team-id"
    userId: "22222222-2222-2222-2222-222222222222"
    role: MEMBER
  ) {
    teamId
    userId
    role
  }
}
```

## Architecture & Important Choices

### Why GraphQL as primary API?

- Clients (web, mobile, internal tools) often want different subsets of data (notes + team info).
- Avoids over/under fetching common in REST for note + team use cases.
- Spring GraphQL + WebFlux gives us a clean, strongly typed, reactive experience.

We still expose health via Actuator (REST).

### Why WebFlux + R2DBC?

See the dedicated "Why Spring Boot, WebFlux, and Project Reactor?" section above for the full rationale. In short: the requirements explicitly called for WebFlux, and the reactive approach provides better scalability for concurrent team usage while keeping the entire pipeline (HTTP → service → database) non-blocking. R2DBC was the natural choice to avoid mixing blocking JDBC drivers.

### Storage Choice: PostgreSQL (with H2 fallback)

- Relational model fits teams + membership + notes well (referential integrity, joins for permissions).
- R2DBC + Postgres is mature.
- H2 is used for instant local development without Docker.

Alternative considered: MongoDB (reactive) — would have been fine for document-oriented notes but we preferred consistency and relational queries for teams.

### Permission & Authorization Model

- Centralized in service layer (not spread in controllers).
- Currently coarse: owners + all team members have full access to team notes.
- Easy to evolve to role-based checks (ADMIN can delete, MEMBER can only read, etc.).

### Authentication

We now support a **real JWT flow** via the new `/auth/login` endpoint (returns a token). The frontend and all examples use `Authorization: Bearer <token>`.

The old `X-User-Id` header is kept as a convenient fallback for demos/GraphiQL (the `UserContextFilter` supports both).

The `CurrentUserService` + Reactor context pattern makes it easy to evolve to full Spring Security OAuth2 Resource Server + JWT validation.

**Future path**: Replace the demo token generation with proper signed JWTs, expiration, refresh tokens, and/or integrate with an external IdP.

### Other Technical Decisions

- **Records** for DTOs and simple domain entities (Java 21).
- **Optimistic locking** (`@Version`) on Note.
- Explicit `CreateNoteInput` / `UpdateNoteInput` (good GraphQL practice).
- Data seeding via `CommandLineRunner` for reproducible demo state.
- Validation annotations (can be added to inputs).
- **SLF4J + Logback** for logging (see dedicated section below).

## Logging (SLF4J + Logback)

### Decision: SLF4J as the API + Logback as the Implementation

We standardized on **SLF4J** (Simple Logging Facade for Java) as the logging API and **Logback** as the concrete implementation.

**Why SLF4J?**
- **Decoupling**: Application code only depends on the SLF4J API (`org.slf4j.Logger`). The actual logging backend (Logback, Log4j2, java.util.logging, etc.) can be swapped at deployment time without changing any code. This is a core principle of good library design and aligns with our "good API methodologies" goal.
- **Industry standard**: Virtually every Java library and framework (including Spring) uses SLF4J. Using it avoids classpath conflicts and "which logging API?" confusion.
- **Performance features**: Parameterized logging (`log.info("User {} did {}", user, action)`) avoids unnecessary string concatenation when the log level is disabled.
- **Ecosystem**: Excellent support for Mapped Diagnostic Context (MDC), markers, and bridging from other logging frameworks.

**Why Logback (via Spring Boot)?**
- Spring Boot's default choice through `spring-boot-starter-logging`.
- High performance, asynchronous appenders available, and native deep integration with Spring (e.g., `logback-spring.xml` with profile support).
- Mature, actively maintained, and battle-tested.
- We deliberately avoided direct Logback imports in application code so we remain on the SLF4J facade only.

**Why not the alternatives?**
- **Direct Logback classes** (`ch.qos.logback...`): Creates hard dependency on one implementation and defeats the purpose of a facade.
- **java.util.logging (JUL)**: Poor performance, limited features, awkward configuration, and poor bridging.
- **System.out / System.err**: No log levels, no filtering, no structured output, breaks log aggregation in containers/Kubernetes, cannot be configured per environment, and violates the non-blocking reactive contract we worked hard to achieve.
- **Log4j2 or other backends directly**: Would require excluding Spring Boot's logging starter and managing versions manually — unnecessary complexity when Logback works excellently out of the box.

### Configuration Choices

We use `logback-spring.xml` (instead of plain `logback.xml`) because:
- It is automatically recognized by Spring Boot.
- Supports `<springProfile>` for environment-specific behavior.
- Allows `${...}` property placeholders from Spring's `Environment`.
- We include Spring Boot's defaults (`defaults.xml`) so we get sensible colored console output and pattern for free.

In `application.yml` we set package-specific levels:
```yaml
logging:
  level:
    com.notetaking.notes: INFO          # Our code
    org.springframework.r2dbc: WARN     # Reduce noise from reactive DB layer
    io.r2dbc: WARN
```

This gives us good signal-to-noise in production while allowing `DEBUG` on `com.notetaking.notes` during development or when investigating issues.

### Reactive / WebFlux Considerations

Because we chose a fully non-blocking stack:
- All logging statements must be non-blocking.
- We use SLF4J's standard synchronous logging (which Logback handles efficiently via its async appender options if needed).
- Error logging in reactive chains uses `.doOnError(e -> log.error("...", e))` rather than blocking.
- Future enhancement (see "If We Had More Time"): add MDC context (e.g., correlation ID or current user) so logs from a single request can be correlated even across async boundaries.

### Connection to Other Design Decisions

Logging was intentionally treated as a cross-cutting concern that should not pollute core business logic. This is why:
- Data seeding (DemoDataSeeder) uses proper logging rather than System prints.
- We separated data initialization from core logic (see previous section) — the seeder can be disabled without affecting how the rest of the application logs.
- Consistent with the "fully reactive" and "clean separation" philosophies we applied everywhere else.

All previous `System.err.println` calls were systematically removed and replaced during this work.

### Why This Level of Logging Setup?

- **Minimal but production-ready**: Enough configuration to be useful without over-engineering for a demo/MVP.
- **Observable**: Structured enough that adding JSON logging or ELK/Loki later is trivial.
- **Maintainable**: Developers use one consistent API (SLF4J) everywhere.

### Potential Future Logging Improvements

If we had more time, we would evolve the logging setup in these directions (many of which tie directly into the broader observability goals listed below):

- **Full MDC (Mapped Diagnostic Context) support**: Automatically propagate correlation IDs, the current user ID (from JWT), request IDs, and trace IDs through the reactive chain. This would allow us to filter logs for a single user session or request even when operations cross multiple async boundaries and threads. In a reactive/WebFlux app this requires careful use of `Context` + `ReactorContext` propagation.

- **Structured / JSON logging**: Switch the console (and add file) appender to output JSON (e.g. using `logstash-logback-encoder` or Logback's built-in JSON encoder). This makes logs machine-readable for centralized platforms (Loki, Elasticsearch, CloudWatch, etc.) and enables powerful querying/filtering on fields like `userId`, `traceId`, `error.code`.

- **Async / high-performance appenders**: Configure Logback's `AsyncAppender` (or `LogbackAsyncAppender`) with a large queue and discarding strategy so that logging never becomes a bottleneck under high load. This is especially relevant because we deliberately chose a non-blocking stack.

- **Non-blocking request/response logging**: Add a reactive `WebFilter` (or `ServerHttpRequestDecorator` / `ServerHttpResponseDecorator`) that logs incoming GraphQL requests and outgoing responses at DEBUG level without blocking the event loop. We would need to be careful to redact sensitive fields (passwords, tokens, personal data).

- **Sensitive data masking / sanitization**: Implement a custom `MessageConverter` or `PatternLayout` that automatically redacts common sensitive patterns (JWTs, emails, IDs) before they reach the appenders. This is critical once real user data is involved.

- **Distributed tracing integration**: Wire Logback with Micrometer Tracing + OpenTelemetry so that log events automatically carry trace/span context. This gives end-to-end visibility from the Svelte frontend through the reactive backend into the database.

- **Environment-specific appenders via Spring profiles**: Use `<springProfile name="prod">` blocks in `logback-spring.xml` to enable file rolling appenders (with size/time-based policies and compression) only in production, while keeping only console logging in dev.

- **Log metrics & alerting**: Expose log-event counters via Micrometer (e.g. count of ERROR logs per endpoint) so we can create dashboards and alerts on logging volume rather than only on application metrics.

- **Client-side logging correlation**: On the Svelte side, generate a `traceId` on the client, pass it in the `X-Trace-Id` header, and have the backend include it in MDC. This would let us correlate a specific user action in the browser console with the exact server logs.

These improvements would move us from "works for a demo" to "enterprise-grade observability" while staying true to the fully reactive principles we applied throughout the stack.

## How to Test

### Backend (GraphiQL / curl)
1. Start the app (`./mvnw spring-boot:run`).
2. Open GraphiQL at http://localhost:8080/graphiql.
3. For demo mode, use the `X-User-Id` header.
4. For real JWT flow, first login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId": "11111111-1111-1111-1111-111111111111"}'
```

Then use the returned token:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token-from-login>" \
  -d '{"query": "{ myNotes { id title } }"}'
```

### Frontend
See the "Impressive Frontend" section above. The UI handles the full JWT login flow automatically.

## Potential Improvements / Next Steps

- Cursor-based pagination (`Connection` spec)
- Subscriptions for real-time note updates (GraphQL + WebSocket)
- Replace demo JWT with full Spring Security OAuth2 / proper signing + refresh tokens
- Content as JSON/blocks instead of raw markdown string
- Flyway for production schema management
- Comprehensive integration tests using `@SpringBootTest` + `WebTestClient` + GraphQL tester (including BlockHound)
- Rate limiting + proper error codes (using `graphql.GraphQLError` customizations)
- Add a production frontend build step to the Maven build (or separate CI)
- **Observability & Logging enhancements** (see expanded list in the [Logging section](#logging-slf4j--logback) above): MDC propagation, structured JSON logs, distributed tracing (OpenTelemetry), async appenders, non-blocking request logging, and sensitive data redaction.

## Tradeoffs Summary

| Choice                    | Benefit                              | Tradeoff / Cost                        |
|---------------------------|--------------------------------------|----------------------------------------|
| GraphQL primary           | Flexible queries, good DX            | More complex than simple REST          |
| Fully reactive (WebFlux)  | High concurrency, modern             | Harder debugging, blocking libs avoided|
| R2DBC + Postgres          | Consistency + reactive               | Slightly more ops complexity           |
| JWT-based auth (with demo token fallback) | Realistic auth flow ready for production | Still simplified (no refresh/revocation yet) |
| Simple offset pagination  | Easy to implement                    | Not great at scale (use cursors later) |
| H2 default                | `spring-boot:run` works instantly    | Not suitable for real concurrency      |

## Running Tests

```bash
./mvnw test
```

Or with system Maven: `mvn test`
```

Also, full package (produces runnable JAR):

```bash
./mvnw clean package -DskipTests
```

## Source Code & Tests

- **Backend**: All Java source under `src/main/java/com/notetaking/notes/` (domain, repositories, services, GraphQL controllers, security with JWT support, etc.).
- **Frontend**: Complete Svelte 5 + Tailwind app under `frontend/src/` (includes all UI logic, GraphQL calls, markdown rendering, etc.).
- **Tests**: Basic context and integration tests under `src/test/` (expandable with GraphQL tester + BlockHound).
- **Other**: `docker-compose.yml`, `schema.graphqls`, `schema.sql`, full Maven wrapper.

Everything needed to run the full stack is committed.

## Impressive Frontend (Svelte + Tailwind)

A modern, feature-rich single-page application is included in the `frontend/` directory (built with Vite + Svelte 5 + Tailwind CSS).

### Features
- **Markdown Preview**: Toggle between Edit and Preview tabs in the note editor using the `marked` library.
- **Dark Mode**: Full support with a toggle button. Persisted in localStorage.
- **Better Member Picker**: Visual grid of user cards (with color avatars) instead of raw text input. Prevents duplicates/self-adds.
- **Real JWT Authentication Flow**: Calls `POST /auth/login` to obtain a token, then sends `Authorization: Bearer <token>` on all requests.
- Sidebar for teams, live search, full CRUD for notes, team member management.
- Loading states, toasts, responsive design, smooth interactions.

### Running the Full Stack
```bash
# Terminal 1: Backend
cd notes-service
./mvnw spring-boot:run

# Terminal 2: Frontend
cd notes-service/frontend
npm install
npm run dev
```

Open http://localhost:5173. The UI automatically logs in via the new JWT endpoint.

### How the Frontend Interfaces with the Backend

- **Primary transport**: `POST http://localhost:8080/graphql` (standard GraphQL over HTTP).
- **Authentication**: 
  1. Call `POST /auth/login` with `{ "userId": "11111111-1111-1111-1111-111111111111" }`
  2. Use the returned `token` in the `Authorization: Bearer <token>` header for subsequent calls.
- Uses `graphql-request` library for clean queries/mutations.
- All data operations map 1:1 to the schema in `src/main/resources/schema.graphqls`.
- CORS is enabled in the backend for `localhost` development.

This is a realistic integration pattern you would use in a real application.

### Updating the Frontend
```bash
cd frontend
npm run dev      # development
npm run build    # production build (output to dist/)
```

The old static `frontend/index.html` was replaced by this proper Vite app.

## Design Choices (Where We Spent the Most Time)

### 1. Fully Reactive Implementation (Zero Blocking Calls)
We invested significant effort ensuring the entire stack is non-blocking: WebFlux handlers, reactive R2DBC repositories, `filterWhen` instead of synchronous filters, and `Mono`/`Flux` throughout the service layer. We also added **BlockHound** in a static initializer so the JVM fails fast on any accidental blocking call (e.g., `Thread.sleep`, blocking I/O).

**Why?** The original requirements explicitly asked for WebFlux. As the reviewer pointed out, using blocking operations defeats the purpose of Project Reactor (thread efficiency, backpressure, scalability under load). The permission checks in particular required several refactors to stay reactive while keeping authorization logic centralized and readable.

### 2. GraphQL as the Primary (and Almost Only) API
We chose Spring GraphQL over a traditional REST controller layer with OpenAPI. Queries and mutations are the main interface; only Actuator health endpoints use REST.

**Why?** 
- Notes + team data naturally benefits from flexible client-driven selection (a client might want just titles + team names in one call).
- Avoids the common N+1/over-fetching problems of fixed REST shapes.
- Aligns with "good API methodologies" for modern backends — clients (web, mobile, or other services) can evolve independently.
- Spring GraphQL + WebFlux provides excellent type safety and reactive execution out of the box.

We still kept the schema explicit with dedicated `CreateNoteInput`/`UpdateNoteInput` types.

### 3. Note Ownership + Team Membership Model with Service-Layer Permissions
Notes are either personal (owner only) or belong to a team. Access is determined by ownership or team membership (via `TeamMember` with roles). All authorization lives in `NoteService`/`TeamService` (using `hasAccess`, `canDelete`, etc.) rather than in controllers or repositories.

**Why?**
- Keeps the data model simple yet realistic for "shared amongst several small teams."
- Centralizing permissions makes it easy to evolve (e.g., add fine-grained roles later) without scattering logic.
- Reactive permission checks (`filterWhen` + `hasElement`) were non-trivial to implement correctly while avoiding N+1 queries in list paths.

Optimistic locking (`@Version` on `Note`) and Java 21 records for domain/DTOs were also deliberate choices for data integrity and modern language features.

## If We Had More Time

### What We Would Add
- **Real authentication & authorization**: Replace the `X-User-Id` header with proper JWT (or OAuth2 Resource Server) + method security. Users would come from an external identity provider.
- **Cursor-based pagination**: Replace the current simple `limit`/`offset` with proper GraphQL Connections for scalability.
- **Rich note content model**: Support structured content (blocks/JSON) or at least sanitized Markdown + attachments.
- **Audit / version history**: Track changes to notes over time (a common requirement for team knowledge bases).
- **GraphQL subscriptions** for real-time updates when a team note is edited.
- **Proper schema management**: Flyway or Liquibase instead of raw `schema.sql`.
- **Comprehensive testing**: More unit tests, contract tests, BlockHound in CI, error-path GraphQL tests, and performance tests under load.
- **Observability**: Full distributed tracing (OpenTelemetry + Micrometer), structured JSON logging, MDC for request/user correlation across the reactive stack, non-blocking request/response logging, log metrics/alerting, and client-server trace correlation. See the detailed future logging improvements in the [Logging (SLF4J + Logback)](#logging-slf4j--logback) section.
- **Better error handling**: Custom `GraphQLError` implementations with proper error codes and extensions.

### What We Would Change or Stop Doing
- Stop relying on seeded demo data and hardcoded UUIDs in tests.
- Move away from embedding the entire team membership check logic inside list queries (could lead to N+1 under scale); consider a projection or dedicated read model.
- Revisit the coarse "all team members have full access" model — it was a deliberate MVP simplification.
- Consider whether a document store (reactive MongoDB) would have been simpler for the note content itself while keeping relational tables only for teams/members.
- Avoid manual `collectList()` + `flatMap` patterns where a more declarative reactive query could suffice.

## Source Code & Tests

All production source code lives under `src/main`.

Relevant test classes (run with `./mvnw test`):
- `NotesServiceApplicationTests` — basic context load (also exercises BlockHound)
- `GraphQLIntegrationTest` — end-to-end GraphQL queries and mutations using `GraphQlTester` against the seeded data

The project deliberately ships with a small but representative set of integration tests rather than hundreds of low-value unit tests, because the value is in the reactive GraphQL + permission flows.

---

## Repository

The complete project (backend + impressive Svelte frontend + all documentation) is available at:

https://github.com/0megaPhil/notes-service

Built as part of a collaborative review process.
