# Task 2 — Agile Planning and DevOps Workflow

## Product backlog

Estimates use story points: 1 = trivial, 2 = half a session, 3 = a full session,
5 = spans two sessions.

| ID | User story | Points | Priority |
|---|---|---|---|
| US-01 | As a speaker I want to create a proposal so that it is officially recorded | 3 | Must |
| US-02 | As any user I want to open a proposal and read its full detail | 2 | Must |
| US-03 | As a speaker I want to edit my proposal so I can correct mistakes | 2 | Must |
| US-04 | As any user I want a list of all proposals so I can see what exists | 2 | Must |
| US-05 | As a reviewer I want to search by keyword so I can find one proposal fast | 3 | Must |
| US-06 | As a reviewer I want to filter by status so I see only what needs me | 2 | Must |
| US-07 | As an organizer I want to filter by track so I can plan rooms | 2 | Should |
| US-08 | As a speaker I want to submit a draft so review can begin | 2 | Must |
| US-09 | As a reviewer I want to approve or reject with a note so the decision is explained | 3 | Must |
| US-10 | As a reviewer I want to send a proposal back so the speaker can improve it | 2 | Should |
| US-11 | As an organizer I want to schedule an approved talk so it joins the programme | 3 | Must |
| US-12 | As an organizer I want to cancel a talk so the schedule stays truthful | 2 | Should |
| US-13 | As a user I want to sign in so my role decides what I can do | 5 | Must |
| US-14 | As an organizer I want a dashboard so I can see the event at a glance | 3 | Must |
| US-15 | As an organizer I want to delete a proposal entered by mistake | 1 | Could |

Total: 37 points.

## Acceptance criteria (examples)

**US-01 — Create a proposal**
- Given I am signed in as a speaker, when I open *New session*, I see an empty form.
- A title shorter than 5 characters is rejected with a message.
- An invalid email address is rejected.
- On save, the proposal exists with status **Draft** and `createdBy` set to me.

**US-09 — Approve or reject**
- Given a proposal is **Submitted**, a reviewer sees Approve, Reject and Send back.
- A speaker viewing the same proposal sees none of those buttons.
- Rejecting with a note stores the note and shows it on the detail page.
- A rejected proposal offers no further actions to anyone.

**US-13 — Role-based sign-in**
- Wrong credentials show an error and do not sign me in.
- An unauthenticated visitor is redirected to the sign-in page.
- A request posted directly to a status endpoint by the wrong role is refused
  by the server, not only hidden in the UI.

## Sprint plan

Two sprints, mapped onto the 13 lab tasks.

**Sprint 1 — working application (tasks 1–6)**
Goal: every MVP function works on a laptop and lives in Git.
US-01, US-02, US-03, US-04, US-13 → then US-05, US-06, US-07, US-08, US-09,
US-10, US-11, US-12, US-14, US-15.

**Sprint 2 — automated delivery (tasks 7–13)**
Goal: a commit becomes a running, tested container without manual steps.
Jenkins build job → Jenkinsfile and deployment → Selenium suite → Selenium in
Jenkins → Docker image → Jenkins-to-Docker delivery → Ansible provisioning and
rollback.

## Kanban board — 15 tasks

| Backlog | In progress | In review | Testing | Done |
|---|---|---|---|---|
| | | | | US-01 … US-15 (all 15 complete) |

Columns used during the work: **Backlog → In progress → In review → Testing → Done**.
Work-in-progress limit of 2 cards in *In progress*, so tasks get finished rather
than started.

## Definition of Done

A story is done only when **all** of these are true:

1. The code is merged into `develop` through a reviewed pull request.
2. The acceptance criteria are demonstrably met in the running application.
3. A unit test covers the rule, and a Selenium test covers the journey if a
   user-visible journey changed.
4. `mvn verify` passes from a clean checkout.
5. The Jenkins pipeline is green for the merge commit.
6. No credentials, build output or database files are committed.
7. The README or the docs folder is updated if behaviour changed.

## DevOps lifecycle

```
   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │   PLAN   │──▶│   CODE   │──▶│  BUILD   │──▶│   TEST   │
   │ backlog, │   │ feature  │   │  Maven   │   │  JUnit,  │
   │  board   │   │ branch,  │   │  package │   │ Selenium │
   └──────────┘   │    PR    │   └──────────┘   └──────────┘
        ▲         └──────────┘         │              │
        │                              ▼              ▼
        │                        ┌──────────┐   ┌──────────┐
        │                        │ RELEASE  │──▶│  DEPLOY  │
        │                        │  Docker  │   │ Jenkins, │
        │                        │  image,  │   │ Ansible  │
        │                        │ registry │   └──────────┘
        │                        └──────────┘         │
        │                                             ▼
   ┌──────────┐                                 ┌──────────┐
   │ MONITOR  │◀────────────────────────────────│ OPERATE  │
   │  health  │                                 │ container│
   │ endpoint │                                 │ restart, │
   │          │                                 │ rollback │
   └──────────┘                                 └──────────┘
```

Where each stage lives in this repository:

| Stage | Implemented by |
|---|---|
| Plan | This document and the GitHub issue templates |
| Code | `src/main/java`, feature branches, pull request template |
| Build | `pom.xml`, Jenkins *Build* stage |
| Test | `src/test/java` — `*Test` unit, `*IT` Selenium |
| Release | `Dockerfile`, versioned image tag per build |
| Deploy | `Jenkinsfile` deploy stage, `ansible/playbook.yml` |
| Operate | `docker restart`, `ansible/rollback.yml` |
| Monitor | `/actuator/health`, container `HEALTHCHECK` |
