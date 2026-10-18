# Tasks 11–12 — Docker Image and Container Lifecycle

## The Dockerfile, stage by stage

**Stage 1 — build.** Starts from `maven:3.9-eclipse-temurin-17`. The `pom.xml`
is copied *before* the source so that `mvn dependency:go-offline` becomes a
cached layer: changing a Java file does not re-download the internet. It then
runs `mvn clean package -DskipITs` — unit tests run here, Selenium does not,
because Selenium needs a browser and a running server.

**Stage 2 — runtime.** Starts from `eclipse-temurin:17-jre` — a JRE only, no
compiler, no Maven, so the shipped image is much smaller. It copies just the WAR
out of the build stage, creates an unprivileged `speaker` user, declares a
volume for the H2 file, and defines a `HEALTHCHECK` that Jenkins and Ansible both
poll.

Why multi-stage: the toolchain needed to *build* the app is not needed to *run*
it, and anything left in the image is extra size and extra attack surface.

## Build and tag

```bash
docker build -t speaker-session-manager:1.0.0 -t speaker-session-manager:latest .
docker image ls speaker-session-manager
docker history speaker-session-manager:1.0.0     # see the layers
```

Two tags from one build: an immutable version (`1.0.0`) that identifies exactly
this code, and a moving `latest` for convenience. The pipeline uses
`1.0.${BUILD_NUMBER}` so every build is traceable to a Jenkins run.

## Container lifecycle — the full set of commands

```bash
# Run detached, map the port, name it so later commands are readable
docker run -d --name speaker-app -p 8080:8080 \
  -e APP_ENV=docker -e SEED_DEMO_DATA=true \
  -v speaker-data:/app/data \
  speaker-session-manager:1.0.0

docker ps                          # running containers, with health status
docker ps -a                       # including stopped ones
docker logs speaker-app            # application output
docker logs -f --tail 50 speaker-app
docker inspect speaker-app         # full configuration as JSON
docker stats --no-stream speaker-app

docker exec -it speaker-app sh     # a shell inside the container
docker port speaker-app            # confirm the port mapping

docker stop speaker-app            # graceful stop (SIGTERM)
docker start speaker-app           # start it again, same filesystem
docker restart speaker-app
docker rm -f speaker-app           # remove it

docker volume ls                   # the named volume outlives the container
docker volume rm speaker-data      # delete the data too
```

Check the health endpoint directly:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

### Using docker compose instead

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f
docker compose down            # stop and remove, keep the volume
docker compose down -v         # and delete the volume
```

## Why a volume

The H2 database is a file at `/app/data`. Without a volume, `docker rm` destroys
every proposal. With `-v speaker-data:/app/data`, the container is disposable and
the data is not — which is the point of treating containers as cattle.

## Task 12 — Jenkins to Docker, end to end

The `Jenkinsfile` turns a commit into a running container:

| Stage | What it does |
|---|---|
| Checkout | Pulls the commit that triggered the build |
| Build | `mvn clean package -DskipITs`, archives the WAR |
| Unit tests | `mvn test`, publishes the JUnit report |
| Docker image | Builds and tags `speaker-session-manager:1.0.${BUILD_NUMBER}` |
| Deploy container | Records the currently running image for rollback, removes the old container, runs the new one, waits for `HEALTHCHECK` to report `healthy` |
| Selenium tests | Runs the suite against that container with `-Dit.base.url` |
| Publish image | Tags and pushes to Docker Hub or a local registry (only when `PUSH_IMAGE` is ticked) |

On failure the `post` block restores the previous image automatically.

### Credentials

The push stage uses `withCredentials` with the id `dockerhub`. Add it in Jenkins
under **Manage Jenkins → Credentials → Global → Add Credentials**, kind
*Username with password*. Never put a registry password in the Jenkinsfile.

### Running a local registry instead of Docker Hub

```bash
docker run -d -p 5000:5000 --name registry registry:2
# then build with REGISTRY = localhost:5000
docker tag speaker-session-manager:1.0.0 localhost:5000/speaker-session-manager:1.0.0
docker push localhost:5000/speaker-session-manager:1.0.0
curl http://localhost:5000/v2/_catalog
```
