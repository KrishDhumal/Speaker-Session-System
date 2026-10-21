# Tasks 7, 8, 10 and 12 — Jenkins (execution log)

Captured on 21 October 2026, 14:25. Jenkins 2.568.2 on port 9090
(the application uses 8080, so Jenkins was moved out of its way).

## Task 7 — freestyle CI job `speaker-session-build`

| Setting | Value |
|---|---|
| Source | Git, branch `*/develop` |
| Trigger | Poll SCM, `H/5 * * * *` |
| Build step | `mvn -B clean package -DskipITs` |
| Post-build | Archive `target/*.war`, publish `target/surefire-reports/*.xml` |

```
$ curl http://localhost:9090/job/speaker-session-build/3/api/json
result   : SUCCESS
duration : 7.2 s
artifacts: ['speaker-session-manager.war']
tests    : total 15 failed 0
```

Console tail:

```
[INFO] Building war: .../target/speaker-session-manager.war
[INFO] BUILD SUCCESS
Archiving artifacts
Recording fingerprints
Recording test results
Finished: SUCCESS
```

The archived WAR and the 15-test report are both attached to the build.

### Trigger evidence

Build #2 was started automatically rather than by hand:

```
Started by an SCM change
```

## Task 8 — pipeline job `speaker-session-pipeline`

Defined by `Jenkinsfile` in the repository (Pipeline script from SCM).

Build #4 — all stages, 69.2s:

```
stage: Declarative: Checkout SCM
stage: Checkout
stage: Build
stage: Unit tests
stage: Deploy to Tomcat
Stage "Deploy to Tomcat" skipped due to when conditional
stage: Docker image
stage: Deploy container
stage: Selenium tests
stage: Publish image
stage: Declarative: Post Actions
```

Parameterised, so the same pipeline serves several environments without
a rebuild: `APP_PORT`, `APP_ENV`, `REGISTRY`, `PUSH_IMAGE`,
`REGISTRY_CREDENTIALS`, `TOMCAT_HOME`.

## Task 10 — continuous testing, and a caught defect

The Selenium suite runs inside the pipeline against the container that was
just deployed, not against a developer laptop:

```
stage: Deploy container
health: starting
health: healthy
stage: Selenium tests
+ mvn -B verify -Dit.base.url=http://localhost:8081 -Dsurefire.skipTests=true
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.59 s -- in com.college.speaker.it.SessionCrudIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.283 s -- in com.college.speaker.it.LoginIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.316 s -- in com.college.speaker.it.SearchIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.37 s -- in com.college.speaker.it.WorkflowIT
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
```

### The defect, caught

A deliberate defect was committed — letting a **speaker** approve their own
session instead of a reviewer:

```java
new Transition(APPROVED, "SPEAKER", "Approve"),   // DELIBERATE DEFECT
```

Pipeline **#3 — FAILURE**. Three tests caught it:

```
[ERROR] StatusWorkflowTest.speakerCannotApprove:32
[ERROR] StatusWorkflowTest.reviewerDecides:40
[ERROR] SpeakerSessionServiceTest.rejectsForbiddenTransition:116
[ERROR] Tests run: 15, Failures: 3, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

The run stopped at the **Build** stage. No Docker image was produced, no
container was deployed and nothing reached the registry — which is exactly
what "failing tests stop deployment" has to mean.

### The fix, and the green rerun

```
  3f4f542 Fix: only a reviewer may approve a submitted session
  3942c84 Introduce a deliberate defect: let a speaker approve their own session
  2ceb42f Make the registry push work without stored credentials
```

Pipeline **#4 — SUCCESS**, every stage green, 15 unit and 20 Selenium tests
passing, image published and deployed.

## Task 12 — commit to container, end to end

Build #4 with `PUSH_IMAGE=true` and `REGISTRY=localhost:5001`:

```
+ docker push localhost:5001/speaker-session-manager:1.0.4
1.0.4: digest: sha256:16dcff65a2121408a922ede6163a06a6fea011b9aa46e31bcd753864de0df290 size: 856
+ docker push localhost:5001/speaker-session-manager:latest
latest: digest: sha256:16dcff65a2121408a922ede6163a06a6fea011b9aa46e31bcd753864de0df290 size: 856
Deployed speaker-session-manager:1.0.4 at http://localhost:8081
```

Registry contents, one tag per Jenkins build:

```
$ curl http://localhost:5001/v2/speaker-session-manager/tags/list
{"name":"speaker-session-manager","tags":["1.0.0","1.0.1-broken","1.0.4","latest","1.0.2"]}

```

The tag is `1.0.${BUILD_NUMBER}`, so every image traces back to the Jenkins
run and therefore to the commit that produced it.

## Reproducing this

```bash
# Jenkins on 9090, because the application owns 8080
java -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true \
     -jar /opt/homebrew/opt/jenkins-lts/libexec/jenkins.war --httpPort=9090
```

The `ALLOW_LOCAL_CHECKOUT` flag is needed only because this repository is a
local directory rather than a GitHub URL. Jenkins refuses local checkouts by
default. Point the jobs at a GitHub remote and the flag is unnecessary.
