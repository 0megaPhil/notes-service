# Team Notes Service

Reactive backend for a note-taking application used and shared by several small teams.

Built with:
- **Java 21**
- **Maven** (wrapper included — `./mvnw` / `.\mvnw.cmd` for zero-install builds)
- **Spring Boot 4.0.6** (fully on Spring Boot 4)
- **Fully reactive** (no `.block()` calls anywhere — pure Project Reactor / WebFlux)
- **BlockHound** installed at startup: the JVM will fail immediately if any blocking call ever sneaks into a reactive path. This is the strongest practical guarantee that WebFlux is actually being used correctly.
- **Spring WebFlux** (fully non-blocking)
- **Spring GraphQL** (primary API)
- **Spring Data R2DBC** (reactive relational access)
- **PostgreSQL** (recommended) / **H2** (zero-config demo)

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

## Running the Service

This project uses **Maven** (wrapper scripts are committed).

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

### Why simple X-User-Id header for auth?

- Allows immediate usability and testing without setting up Keycloak / Auth0.
- Clearly documented as a placeholder.
- The `CurrentUserService` + Reactor context pattern is easy to swap for a real JWT decoder later.

**Future path**: Replace filter + header with `ServerHttpSecurity.oauth2ResourceServer().jwt()` + proper claims.

### Other Technical Decisions

- **Records** for DTOs and simple domain entities (Java 21).
- **Optimistic locking** (`@Version`) on Note.
- Explicit `CreateNoteInput` / `UpdateNoteInput` (good GraphQL practice).
- Data seeding via `CommandLineRunner` for reproducible demo state.
- Validation annotations (can be added to inputs).

## How to Test

1. Start the app.
2. Open GraphiQL.
3. Send a query with the `X-User-Id` header (GraphiQL supports "Request Headers" panel in newer versions, or use curl/Postman).

Example curl:

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{"query": "{ myNotes { id title } }"}'
```

## Potential Improvements / Next Steps

- Cursor-based pagination (`Connection` spec)
- Subscriptions for real-time note updates
- Full JWT / OAuth2 integration
- Content as JSON/blocks instead of raw markdown string
- Flyway for production schema management
- Comprehensive integration tests using `@SpringBootTest` + `WebTestClient` + GraphQL tester
- Rate limiting + proper error codes (using `graphql.GraphQLError` customizations)

## Tradeoffs Summary

| Choice                    | Benefit                              | Tradeoff / Cost                        |
|---------------------------|--------------------------------------|----------------------------------------|
| GraphQL primary           | Flexible queries, good DX            | More complex than simple REST          |
| Fully reactive (WebFlux)  | High concurrency, modern             | Harder debugging, blocking libs avoided|
| R2DBC + Postgres          | Consistency + reactive               | Slightly more ops complexity           |
| Header-based demo auth    | Fast to start                        | Not production ready                   |
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
- **Observability**: Distributed tracing, metrics on GraphQL operation latency, and structured error logging.
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

Built as part of a collaborative review process. All source code, tests, and this documentation are committed and pushed to the repository.
