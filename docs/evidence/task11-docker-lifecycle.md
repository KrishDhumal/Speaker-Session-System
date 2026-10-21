# Task 11 — Docker Image and Container Lifecycle (execution log)

Captured on 21 October 2026, 14:05.

## Image build

```
$ docker build -t speaker-session-manager:1.0.0 -t speaker-session-manager:latest .
[... multi-stage build: maven builder, then JRE runtime ...]
naming to docker.io/library/speaker-session-manager:1.0.0 done
```

## Image details

```
$ docker image ls speaker-session-manager
IMAGE                            ID             DISK USAGE   CONTENT SIZE   EXTRA
speaker-session-manager:1.0.0    bc61eee637af        663MB          210MB        
speaker-session-manager:latest   bc61eee637af        663MB          210MB        
```

### Layer history (note the two stages)

```
$ docker history speaker-session-manager:1.0.0 --format "{{.CreatedBy}}	{{.Size}}" --no-trunc
0B	ENTRYPOINT ["sh" "-c" "exec java $JAVA_OPTS …
0B	HEALTHCHECK &{["CMD-SHELL" "curl --fail --si…
0B	ENV APP_ENV=docker JAVA_OPTS=-XX:MaxRAMPerce…
0B	EXPOSE [8080/tcp]
0B	USER speaker
0B	VOLUME [/app/data]
56.5MB	RUN /bin/sh -c mkdir -p /app/data && chown -…
56.5MB	COPY /build/target/speaker-session-manager.w…
8.19kB	WORKDIR /app
41kB	RUN /bin/sh -c groupadd --system speaker && …
4.1kB	RUN /bin/sh -c apt-get update     && apt-get…
0B	ENTRYPOINT ["/__cacert_entrypoint.sh"]
```

## Container lifecycle

### Run detached with a port mapping and a named volume

```
$ docker run -d --name speaker-app -p 8081:8080 \
    -e APP_ENV=docker -e SEED_DEMO_DATA=true \
    -v speaker-data:/app/data speaker-session-manager:1.0.0
01ca0939495c1bd8b7073a430b740f712a255efb7c558106834b4bef2925e119
```

### Running container, with health status

```
$ docker ps
NAMES         IMAGE                           STATUS                    PORTS
speaker-app   speaker-session-manager:1.0.0   Up 19 seconds (healthy)   0.0.0.0:8081->8080/tcp, [::]:8081->8080/tcp
```

### Health endpoint answering through the mapped port

```
$ curl http://localhost:8081/actuator/health
{"status":"UP","groups":["liveness","readiness"]}
$ curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/sessions   # redirects to sign-in
200
```

### Port mapping

```
$ docker port speaker-app
8080/tcp -> 0.0.0.0:8081
8080/tcp -> [::]:8081
```

### Application logs

```
$ docker logs --tail 6 speaker-app
2026-10-21T08:35:52.885Z  INFO 1 --- [speaker-session-manager] [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpo
2026-10-21T08:35:53.110Z  INFO 1 --- [speaker-session-manager] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started o
2026-10-21T08:35:53.122Z  INFO 1 --- [speaker-session-manager] [           main] c.c.speaker.SpeakerSessionApplication    : Started SpeakerS
2026-10-21T08:35:54.057Z  INFO 1 --- [speaker-session-manager] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spr
2026-10-21T08:35:54.057Z  INFO 1 --- [speaker-session-manager] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Ser
2026-10-21T08:35:54.057Z  INFO 1 --- [speaker-session-manager] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initia
```

### Running as a non-root user

```
$ docker exec speaker-app id
uid=999(speaker) gid=999(speaker) groups=999(speaker)
```

### Stop, start, restart

```
$ docker stop speaker-app
speaker-app
$ docker ps -a --filter name=speaker-app   # still exists, now exited
NAMES         STATUS
speaker-app   Exited (143) Less than a second ago

$ docker start speaker-app
speaker-app
$ docker restart speaker-app
speaker-app
```

### The volume outlives the container

The H2 database file lives on the named volume, so destroying the
container does not destroy the data.

```
$ docker exec speaker-app ls -l /app/data
total 36
-rw-r--r-- 1 speaker speaker 36864 Oct 21 08:37 speakerdb.mv.db

$ docker rm -f speaker-app        # container destroyed
speaker-app

$ docker volume ls --filter name=speaker-data
DRIVER    VOLUME NAME
local     speaker-data

$ docker run -d --name speaker-app -p 8081:8080 -v speaker-data:/app/data speaker-session-manager:1.0.0
cd8ae0a3e4c8c761f67b152908423916508c5d22cbc4b2e3961edebdec49edae
```

After recreating the container, the database file is still there:

```
$ docker exec speaker-app ls -l /app/data
total 24
-rw-r--r-- 1 speaker speaker 24576 Oct 21 08:38 speakerdb.mv.db
```

### Full command reference used

```bash
docker build -t speaker-session-manager:1.0.0 -t speaker-session-manager:latest .
docker image ls speaker-session-manager
docker history speaker-session-manager:1.0.0
docker run -d --name speaker-app -p 8081:8080 -v speaker-data:/app/data speaker-session-manager:1.0.0
docker ps                    # running, with health
docker ps -a                 # including stopped
docker port speaker-app      # port mapping
docker logs --tail 50 speaker-app
docker exec speaker-app id   # confirms the non-root user
docker inspect speaker-app
docker stop speaker-app
docker start speaker-app
docker restart speaker-app
docker rm -f speaker-app
docker volume ls
```
