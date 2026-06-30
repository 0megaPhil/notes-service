# Team Notes Service

Reactive backend for a note-taking application used and shared by several small teams.

Built with:
- **Java 21**
- **Maven** (wrapper included — `./mvnw` / `.\mvnw.cmd` for zero-install builds)
- **Spring Boot 4.0.6** (fully on Spring Boot 4)
- **Spring WebFlux** (fully non-blocking)
- **Spring GraphQL** (primary API)
- **Spring Data R2DBC** (reactive relational access)
- **PostgreSQL** (recommended) / **H2** (zero-config demo)

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

### Why WebFlux + R2DBC instead of traditional Spring MVC + JPA?

- The requirement explicitly asked for **WebFlux**.
- Notes services are read-heavy with occasional writes. Reactive model shines for I/O bound workloads and high concurrency with low threads.
- R2DBC keeps the stack fully reactive end-to-end.

**Tradeoff**: Steeper learning curve, more complex debugging, and some libraries have weaker support vs blocking stack. For a small team service this is acceptable.

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

## Review Notes for Reviewers

This is intentionally an opinionated MVP. I'm ready to discuss:

- Why not REST + OpenAPI instead of (or in addition to) GraphQL?
- Whether we should have used Spring Data MongoDB Reactive.
- How much auth should have been implemented.
- Performance characteristics of the current permission checks (N+1 potential).

Please provide feedback and I will iterate quickly.

---

Built as part of a collaborative review process.
