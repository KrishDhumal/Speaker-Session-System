# Tasks 6–7 — Jenkins Installation and Pipeline

## Installing Jenkins

**macOS**

```bash
brew install jenkins-lts
brew services start jenkins-lts
```

Jenkins then runs on <http://localhost:8080>. The application's default port is
also 8080, so run the app on a different port while Jenkins is up — the pipeline
deploys the container to `8081` for exactly this reason.

The first-run unlock password:

```bash
cat ~/.jenkins/secrets/initialAdminPassword
```

Install the suggested plugins, then add these if they are not already present:
**Pipeline**, **Git**, **Maven Integration**, **JUnit**, **Docker Pipeline**.

**Tool configuration** — *Manage Jenkins → Tools*: add a JDK (17) and a Maven
installation, or tick *Install automatically*.

## Task 6 — a freestyle build job

This is the simpler job, worth building first to show a plain CI build:

1. *New Item → Freestyle project*, name it `speaker-session-build`.
2. **Source Code Management → Git**, repository URL = your GitHub repository,
   branch `*/develop`.
3. **Build Triggers → Poll SCM**, schedule `H/5 * * * *` — Jenkins asks Git
   every five minutes whether there are new commits. With a public GitHub
   repository you can instead use *GitHub hook trigger* and a webhook.
4. **Build Steps → Invoke top-level Maven targets**, goals:
   `clean package -DskipITs`.
5. **Post-build Actions**:
   - *Archive the artifacts*: `target/*.war`
   - *Publish JUnit test result report*: `target/surefire-reports/*.xml`

Run it. The console log shows the Maven build, and the build page then offers the
WAR as a downloadable artefact — that is the evidence for Task 6.

## Task 7 — the pipeline job

1. *New Item → Pipeline*, name it `speaker-session-pipeline`.
2. **Pipeline → Definition**: *Pipeline script from SCM*.
3. SCM *Git*, your repository, branch `*/develop`,
   **Script Path**: `Jenkinsfile`.
4. Save, then *Build with Parameters*.

### Parameters

| Parameter | Default | Purpose |
|---|---|---|
| `APP_PORT` | `8081` | Host port for the deployed container |
| `APP_ENV` | `ci` | Shown in the application footer, so you can see which build you are looking at |
| `REGISTRY` | `docker.io/<user>` | Where to push the image |
| `PUSH_IMAGE` | `false` | Only push when credentials are configured |

`APP_ENV` is the parameterised environment setting Task 7 asks for: the same
image is deployed to different environments by changing a parameter, never by
rebuilding.

### Deploying to Tomcat instead of Docker

The artefact is a WAR precisely so this is possible. Drop it into a standalone
Tomcat:

```bash
cp target/speaker-session-manager.war $CATALINA_HOME/webapps/
# the application is then at http://localhost:8080/speaker-session-manager/
```

Or with the *Deploy to container* Jenkins plugin, add a post-build action
pointing at the Tomcat manager URL with a credentials id.

## Reading a pipeline run

The pipeline's stage view shows each stage as a column. A red stage stops
everything after it, which is the behaviour Task 9 asks for: failing tests must
block deployment.

Useful artefacts on every build page:

- `target/*.war` — the exact artefact that was built
- *Test Result* — unit tests, and Selenium results once that stage runs
- `target/selenium-screenshots/*` — PNG and HTML for any failed journey
