# Speaker Session Management System

A small web application for running the talk-proposal process of a conference:
speakers submit sessions, reviewers approve or reject them, and organizers put
the approved ones on the schedule.

It is the application under test for a DevOps pipeline exercise — Git, Jenkins,
Selenium, Docker and Ansible all build on this codebase.

## Stack

| Layer | Choice |
|---|---|
| Language / framework | Java 17, Spring Boot 3.3 |
| Build tool | Maven (`pom.xml`) |
| UI | Thymeleaf server-rendered HTML + plain CSS |
| Persistence | Spring Data JPA over H2 (file-based) |
| Security | Spring Security, form login, three in-memory roles |
| Packaging | WAR — runs with `java -jar` *or* deploys into a standalone Tomcat |
| Tests | JUnit 5 (unit) and Selenium WebDriver (end-to-end) |

Nothing has to be installed beyond a JDK and Maven. The database is a file under
`data/`, created on first run.

## Running it locally

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>. You are redirected to a sign-in page.

### Demo accounts

| Username | Role | Can do |
|---|---|---|
| `speaker` | SPEAKER | create and edit sessions, submit for review |
| `reviewer` | REVIEWER | approve, reject, send back for changes |
| `organizer` | ORGANIZER + REVIEWER | schedule, cancel, delete |

Passwords are development defaults set in
[`application.properties`](src/main/resources/application.properties) and can be
overridden with the `SPEAKER_PASSWORD`, `REVIEWER_PASSWORD` and
`ORGANIZER_PASSWORD` environment variables. Change them for any deployment that
is reachable by anyone else.

## The MVP

1. **Create** a session — title, speaker, track, room, start time, duration, abstract.
2. **View** a single session with its full detail and the actions open to you.
3. **Update** a session's details.
4. **Search** by keyword, status and track; all three filters are optional and combine.
5. **Role-based status workflow** — see below.
6. **Summary dashboard** — totals per status, sessions per track, next scheduled talks.

### Status workflow

```
            speaker              reviewer                 organizer
  DRAFT ──── submit ────► SUBMITTED ──── approve ────► APPROVED ──── schedule ────► SCHEDULED
    ▲                         │                           │                             │
    └──── send back ──────────┤                           └──── cancel ────► CANCELLED ◄┘
                              └──── reject ────► REJECTED
```

`REJECTED` and `CANCELLED` are dead ends. Every rule lives in one class,
[`StatusWorkflow`](src/main/java/com/college/speaker/service/StatusWorkflow.java) —
the controller and the templates both ask it what is allowed, so a button never
appears for an action the current user cannot perform.

## Project layout

```
src/main/java/com/college/speaker/
├── SpeakerSessionApplication.java   entry point
├── ServletInitializer.java          lets the WAR run inside a standalone Tomcat
├── config/                          security, demo data seeding
├── domain/                          SpeakerSession entity, SessionStatus, Track
├── repo/                            Spring Data repository + the search query
├── service/                         business rules, workflow, exceptions
└── web/                             controllers
src/main/resources/
├── application.properties           all configuration, env-var overridable
├── templates/                       Thymeleaf views
└── static/css/app.css
src/test/java/                       unit tests (*Test) and Selenium tests (*IT)
```

## Build commands

```bash
mvn test                  # unit tests only — fast, no browser needed
mvn verify                # unit tests + Selenium end-to-end tests
mvn clean package         # produces target/speaker-session-manager.war
java -jar target/speaker-session-manager.war
```

## Configuration

Every setting reads an environment variable first, so the same artefact runs in
every environment without a rebuild.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:h2:file:./data/speakerdb` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `sa` / empty | database credentials |
| `SEED_DEMO_DATA` | `true` | insert sample rows into an empty database |
| `APP_ENV` | `local` | label shown in the page footer |
| `SPEAKER_PASSWORD` etc. | see above | demo account passwords |

`GET /actuator/health` returns the health check used by the deployment scripts.

## Branching policy

| Branch | Purpose |
|---|---|
| `main` | release-ready; tagged for each version |
| `develop` | integration branch; features merge here first |
| `feature/<short-name>` | one branch per feature, merged by pull request |
| `bugfix/<short-name>` | fixes for defects found in testing |

Commit messages use the imperative mood and name the change, e.g.
`Add track filter to session search`.

## DevOps pipeline

| Concern | File | Run it with |
|---|---|---|
| Container image | [`Dockerfile`](Dockerfile) | `docker build -t speaker-session-manager:1.0.0 .` |
| Local container | [`docker-compose.yml`](docker-compose.yml) | `docker compose up --build` |
| CI/CD | [`Jenkinsfile`](Jenkinsfile) | Pipeline job, script path `Jenkinsfile` |
| Provisioning | [`ansible/playbook.yml`](ansible/playbook.yml) | `cd ansible && ansible-playbook playbook.yml` |
| Rollback | [`ansible/rollback.yml`](ansible/rollback.yml) | `cd ansible && ansible-playbook rollback.yml` |

## Documentation

Every written deliverable lives in [`docs/`](docs/README.md), which indexes
each of the thirteen project tasks against the file that answers it.

| | |
|---|---|
| [Problem and scope](docs/01-problem-and-scope.md) | Stakeholders, objectives, constraints, the 15-item MVP |
| [Agile plan](docs/02-agile-plan.md) | Backlog, sprints, Definition of Done, DevOps lifecycle |
| [SRS and architecture](docs/03-srs-and-architecture.md) | Requirements, diagrams, data model, endpoints |
| [Git workflow](docs/04-git-workflow.md) | Branch policy, pull requests, conflicts, tagging |
| [Test plan](docs/05-test-plan.md) | Test levels, the five journeys, continuous testing |
| [Docker guide](docs/06-docker-guide.md) | Image build, container lifecycle, registry |
| [Jenkins setup](docs/07-jenkins-setup.md) | Installation, freestyle job, pipeline job |
| [Ansible guide](docs/08-ansible-guide.md) | Prerequisites, idempotency, health check, rollback |
