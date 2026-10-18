# Tasks 9–10 — Test Plan and Continuous Testing

## Test levels

| Level | Tool | Files | Runs on |
|---|---|---|---|
| Unit | JUnit 5 + AssertJ | `*Test.java` | `mvn test`, every build, ~3 seconds |
| End-to-end | Selenium WebDriver | `*IT.java` | `mvn verify`, against a browser, ~40 seconds |

Surefire runs the unit tests; Failsafe runs the Selenium tests. Keeping them
separate means a broken browser never blocks a quick unit-test run.

## Unit test coverage (15 tests)

**`StatusWorkflowTest`** — the role rules, with no Spring context at all:
a speaker can submit a draft; a speaker cannot approve; a reviewer gets exactly
approve/reject/send-back; only an organizer can schedule; a draft cannot jump
straight to scheduled; rejected and cancelled are dead ends.

**`SpeakerSessionServiceTest`** — the service against a real in-memory database,
so the search query itself is exercised: a new session always starts as Draft;
keyword search matches title or speaker, case-insensitively; empty filters return
everything; status and track filters combine with the keyword; update changes the
details but never the status; a forbidden transition throws; a review note is
stored; a missing id raises a 404; the dashboard counts every status and track.

## The five critical user journeys (Selenium, 20 tests)

| # | Journey | Class | Why it matters |
|---|---|---|---|
| 1 | Sign in, and see only what your role allows | `LoginIT` | Everything else depends on authentication working |
| 2 | Create, view and edit a proposal | `SessionCrudIT` | The core of the application |
| 3 | Find a proposal again through search | `SearchIT` | The feature reviewers use most |
| 4 | Draft → Submitted → Approved → Scheduled | `WorkflowIT` | The whole point of the system |
| 5 | A forged request from the wrong role is refused | `WorkflowIT` | Proves authorisation is server-side |

### Test data

Tests create their own data through the user interface and give every record a
unique title (`uniqueTitle()` appends a nanosecond timestamp), so tests never
collide and can run in any order. Demo-data seeding is switched off for the test
run with `app.seed-demo-data=false`.

### Failure evidence

`ScreenshotOnFailure` is a JUnit extension that fires on any failed Selenium
test and writes to `target/selenium-screenshots/`:

- a **PNG** of exactly what the browser was showing,
- the **page HTML**, which explains failures a picture cannot (a redirect to an
  error page, a missing CSRF field),
- the **value of every form field**, printed to the build log.

It runs as an `afterTestExecution` callback rather than a `TestWatcher`, because
a watcher fires after `@AfterEach` — by which point the browser is closed and
there is nothing left to photograph.

Jenkins archives that whole folder, so a failed pipeline carries its own evidence.

### Reliability decisions worth explaining

These were not chosen in advance; each one is a bug that was found and fixed
while getting the suite to pass:

| Problem seen | Fix |
|---|---|
| Tests passed alone, failed together | A fresh browser per test, so no signed-in session or cached page leaks into the next test |
| Assertions read the previous page's rows | `click()` tags the document, then waits for a document without the tag — this also catches a form re-rendering at the *same* URL |
| A click was accepted but never delivered | `click()` retries, falling back to dispatching the click from the page itself |
| `sendKeys` silently typed nothing | `type()` reads the value back and retries; `clear()` leaves the field unfocused in current Chrome |
| Chrome 155 dropped input entirely | The browser version is **pinned** — Selenium downloads a known-good *Chrome for Testing* build, so the laptop and the Jenkins agent behave identically |
| Mixed implicit and explicit waits timed out unpredictably | The implicit wait was removed; everything waits explicitly |

A test asserting a *malformed* email is rejected was also rewritten: Chrome
blocks submission of an invalid `<input type="email">` itself, so the server-side
rule under test was never reached. The test now leaves the field empty.

## Running the tests

```bash
mvn test                                   # unit only
mvn verify                                 # unit + Selenium, app started in-process
mvn verify -Dit.base.url=http://host:8081  # Selenium against a deployed container
mvn verify -Dit.headless=false             # watch the browser work
mvn verify -DskipITs                       # skip Selenium
```

## Task 10 — continuous testing in Jenkins

The pipeline wires the suite in so that **a failing test stops the deployment**:

1. *Unit tests* stage runs `mvn test` and publishes `surefire-reports/*.xml`.
2. *Deploy container* starts the new image and waits for its health check.
3. *Selenium tests* stage runs the suite against that container with
   `-Dit.base.url`, and publishes `failsafe-reports/*.xml`.
4. Because a non-zero Maven exit fails the stage, the *Publish image* stage
   never runs, and the `post { failure { ... } }` block rolls the container back
   to the previous image.

### Demonstrating a caught defect

To show the pipeline catching a real bug, introduce one deliberately — for
example, let a speaker approve their own session by adding `"SPEAKER"` to the
approve transition in `StatusWorkflow`:

```java
new Transition(APPROVED, "SPEAKER", "Approve"),   // deliberate defect
```

Commit and push. The pipeline fails at the *Unit tests* stage
(`speakerCannotApprove`) and never deploys. Revert the line, commit again, and
the pipeline goes green — that pair of builds is the evidence for Task 10.
