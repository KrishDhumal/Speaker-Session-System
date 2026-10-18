# Task 3 — Requirements, Architecture and Technology

## SRS summary

### Functional requirements

| ID | Requirement |
|---|---|
| FR-1 | The system shall let a speaker create a session proposal with title, speaker name, email, track, optional room, start time, duration and abstract. |
| FR-2 | The system shall validate input: title 5–120 characters, a valid email, a track, a start time, duration 15–240 minutes, abstract at most 2000 characters. |
| FR-3 | The system shall display a single proposal with all its fields and its current status. |
| FR-4 | The system shall let a speaker update the editable fields of a proposal. |
| FR-5 | The system shall let an organizer delete a proposal. |
| FR-6 | The system shall list all proposals ordered by start time. |
| FR-7 | The system shall search proposals by keyword against title and speaker name, case-insensitively. |
| FR-8 | The system shall filter proposals by status and by track, combinable with the keyword. |
| FR-9 | The system shall record each proposal in exactly one of six states: Draft, Submitted, Approved, Rejected, Scheduled, Cancelled. |
| FR-10 | The system shall permit a status change only if the user's role is allowed to make that specific transition. |
| FR-11 | The system shall store an optional note with a status change and display it. |
| FR-12 | The system shall authenticate users and assign one or more of the roles SPEAKER, REVIEWER, ORGANIZER. |
| FR-13 | The system shall present a dashboard with total proposals, counts per status, counts per track and the next scheduled sessions. |
| FR-14 | The system shall expose a health endpoint for automated deployment checks. |

### Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-1 | **Security** — authorisation is enforced on the server; hiding a button is not sufficient. All forms are CSRF-protected. |
| NFR-2 | **Portability** — the same artefact runs on a laptop, in Docker and under Tomcat, configured only by environment variables. |
| NFR-3 | **Testability** — every page element used by a test carries a stable `id`. |
| NFR-4 | **Performance** — a search over the expected data volume (a few hundred rows) returns in well under a second; it is a single indexed query, not an in-memory filter. |
| NFR-5 | **Maintainability** — the workflow rules live in exactly one class. |
| NFR-6 | **Recoverability** — a failed release can be returned to the previous image without rebuilding. |

## Use-case diagram

```
                 Speaker Session Management System
   ┌─────────────────────────────────────────────────────────┐
   │                                                         │
   │   ( Create proposal )        ( Approve proposal )       │
   │   ( Edit proposal )          ( Reject proposal )        │
   │   ( Submit for review )      ( Send back )              │
   │                                                         │
   │   ( Search / filter )        ( Schedule session )       │
   │   ( View proposal )          ( Cancel session )         │
   │   ( View dashboard )         ( Delete proposal )        │
   │                                                         │
   └─────────────────────────────────────────────────────────┘
        ▲                  ▲                      ▲
        │                  │                      │
    ┌───┴────┐        ┌────┴─────┐          ┌─────┴─────┐
    │Speaker │        │ Reviewer │          │ Organizer │
    └────────┘        └──────────┘          └───────────┘
         │                  │                     │
         └──── all three: sign in, view, search ──┘
```

## Architecture

Layered Spring MVC — each layer talks only to the one below it.

```
   Browser (HTML + CSS, no JavaScript framework)
        │  HTTP
        ▼
   ┌──────────────────────────────────────────┐
   │  Web layer                               │
   │  SessionController, DashboardController  │  ← validates input, chooses view
   │  Thymeleaf templates                     │
   └──────────────────────────────────────────┘
        │
        ▼
   ┌──────────────────────────────────────────┐
   │  Service layer                           │
   │  SpeakerSessionService                   │  ← transactions, business rules
   │  StatusWorkflow  ← the only place role   │
   │                    rules are defined     │
   └──────────────────────────────────────────┘
        │
        ▼
   ┌──────────────────────────────────────────┐
   │  Repository layer                        │
   │  SpeakerSessionRepository (Spring Data)  │  ← one search query
   └──────────────────────────────────────────┘
        │  JPA / Hibernate
        ▼
   ┌──────────────────────────────────────────┐
   │  H2 database (file) — swappable for      │
   │  MySQL by changing DB_URL only           │
   └──────────────────────────────────────────┘

   Cross-cutting: Spring Security filter chain (authentication, roles, CSRF)
                  Spring Actuator (/actuator/health)
```

## Data model

**Table `speaker_sessions`**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | primary key, auto-increment |
| title | VARCHAR(120) | not null, 5–120 characters |
| speaker_name | VARCHAR(80) | not null |
| speaker_email | VARCHAR(120) | not null, valid email |
| track | VARCHAR(20) | enum: DEVOPS, CLOUD, AI_ML, SECURITY, WEB, OTHER |
| room | VARCHAR(40) | nullable |
| start_time | TIMESTAMP | not null |
| duration_minutes | INT | 15–240 |
| summary | VARCHAR(2000) | the abstract, nullable |
| status | VARCHAR(20) | enum: DRAFT, SUBMITTED, APPROVED, REJECTED, SCHEDULED, CANCELLED |
| created_by | VARCHAR(60) | username of the creator |
| review_note | VARCHAR(500) | reason recorded with a status change |
| created_at | TIMESTAMP | set on insert |
| updated_at | TIMESTAMP | set on every update |

Users are not a database table in the MVP — three accounts are defined in
`SecurityConfig` so the exercise stays focused on the workflow.

## Endpoint list

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/` | any | redirect to the dashboard |
| GET | `/dashboard` | authenticated | summary counts and upcoming sessions |
| GET | `/login` | public | sign-in form |
| POST | `/login` | public | authenticate |
| POST | `/logout` | authenticated | end the session |
| GET | `/sessions` | authenticated | list and search (`keyword`, `status`, `track`) |
| GET | `/sessions/new` | SPEAKER | empty create form |
| POST | `/sessions/new` | SPEAKER | create a proposal |
| GET | `/sessions/{id}` | authenticated | detail plus the actions allowed to this role |
| GET | `/sessions/{id}/edit` | SPEAKER | edit form |
| POST | `/sessions/{id}/edit` | SPEAKER | save changes |
| POST | `/sessions/{id}/status` | role per transition | move the status |
| POST | `/sessions/{id}/delete` | ORGANIZER | delete |
| GET | `/actuator/health` | public | health probe for Docker and Ansible |

## Technology choices and why

| Decision | Chosen | Reasoning |
|---|---|---|
| Language / framework | Java 17, Spring Boot 3.3 | Already known; the smallest learning cost for the application half. |
| Build tool | **Maven** | Already known; one `pom.xml`, and Jenkins has first-class support. |
| UI | Thymeleaf server-side HTML | No npm toolchain to learn or install, and Selenium gets plain HTML with stable ids. A React front end would have added a second build. |
| Database | H2, file-based | Zero installation, real SQL, real JPA. `DB_URL` points at MySQL later with no code change. |
| Packaging | WAR | Runs with `java -jar` *and* deploys into a standalone Tomcat, so the "deploy to Tomcat" requirement is met without a second artefact. |
| Config management | **Ansible** rather than Puppet | Agentless — it works over plain SSH, so there is no master/agent to install and keep running. The playbook is one YAML file that reads top to bottom. |
| Container | Docker multi-stage | The build stage holds the JDK and Maven; the runtime image ships only a JRE, so the published image is far smaller. |

## Local development setup

```bash
mvn spring-boot:run      # http://localhost:8080
mvn test                 # unit tests, a few seconds
mvn verify               # unit + Selenium
```

Requirements: JDK 17 or newer and Maven. Selenium downloads its own browser, so
nothing else needs installing.
