# Task 12 — Jenkins-Docker Continuous Deployment (execution log)

Captured on 21 October 2026, 14:08.

A local registry is used instead of Docker Hub so the demonstration needs
no account and no credentials. The pipeline pushes to whatever `REGISTRY`
points at, so switching to Docker Hub is a parameter change.

## Start a local registry

```
$ docker run -d -p 5001:5000 --name registry registry:2
$ curl http://localhost:5001/v2/_catalog
{"repositories":[]}
```

## Tag two versions and push them

```
$ docker tag speaker-session-manager:1.0.0 localhost:5001/speaker-session-manager:1.0.0
$ docker tag speaker-session-manager:1.0.0 localhost:5001/speaker-session-manager:latest
$ docker push localhost:5001/speaker-session-manager:1.0.0
99d6970661fe: Pushed
10b0d84fa8f8: Pushed
1.0.0: digest: sha256:bc61eee637af5a5d737d1993a9a58cdf8fa25a3e505ebcf51d69e5061a73f108 size: 856
$ docker push localhost:5001/speaker-session-manager:latest
3da024eb9de7: Layer already exists
latest: digest: sha256:bc61eee637af5a5d737d1993a9a58cdf8fa25a3e505ebcf51d69e5061a73f108 size: 856
```

## Registry contents after the push

```
$ curl http://localhost:5001/v2/_catalog
{"repositories":["speaker-session-manager"]}

$ curl http://localhost:5001/v2/speaker-session-manager/tags/list
{"name":"speaker-session-manager","tags":["1.0.0","latest"]}

```

## Deploy a fresh container from the registry

This is the last step of the pipeline: nothing is built locally, the image
is pulled from the registry exactly as a deployment server would.

```
$ docker rmi localhost:5001/speaker-session-manager:1.0.0   # prove the pull is real
Untagged: localhost:5001/speaker-session-manager:1.0.0
$ docker run -d --name speaker-app -p 8081:8080 -v speaker-data:/app/data localhost:5001/speaker-session-manager:1.0.0
ed8232b779384be88507b017245c4a5888f6f1e3b8eb4a900a7a8fe2b03b2e50
```

The image was genuinely pulled, not reused from the local cache:

```
Unable to find image 'localhost:5001/speaker-session-manager:1.0.0' locally
1.0.0: Pulling from speaker-session-manager
Status: Downloaded newer image for localhost:5001/speaker-session-manager:1.0.0
```

```
$ docker ps
NAMES         IMAGE                                          STATUS                    PORTS
speaker-app   localhost:5001/speaker-session-manager:1.0.0   Up 55 seconds (healthy)   0.0.0.0:8081->8080/tcp, [::]:8081->8080/tcp
registry      registry:2                                     Up About a minute         0.0.0.0:5001->5000/tcp, [::]:5001->5000/tcp

$ curl http://localhost:8081/actuator/health
{"status":"UP","groups":["liveness","readiness"]}
```

## How the pipeline automates this

The `Jenkinsfile` does the same sequence on every commit:

| Stage | Command |
|---|---|
| Docker image | `docker build -t speaker-session-manager:1.0.${BUILD_NUMBER} .` |
| Deploy container | records the running image, then `docker run` the new tag and waits for HEALTHCHECK |
| Selenium tests | `mvn verify -Dit.base.url=http://localhost:8081` |
| Publish image | `docker push ${REGISTRY}/speaker-session-manager:1.0.${BUILD_NUMBER}` |

The version tag is `1.0.${BUILD_NUMBER}`, so every image traces back to the
Jenkins run and therefore to the commit that produced it. The publish stage
runs only after the Selenium stage passes, and `post { failure { ... } }`
restores the previously running image if any stage fails.
