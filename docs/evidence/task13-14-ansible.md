# Tasks 13–14 — Configuration Management and Provisioning (execution log)

Captured on 21 October 2026, 14:12. Ansible core 2.21.5.

## Task 13 — first execution

Deploying to `$HOME/speaker-session-manager` rather than `/opt`, so the
demonstration needs no sudo. For a real server, set `app_dir_override` in
the inventory and run with `--become`.

```
$ cd ansible && ansible-playbook playbook.yml

TASK [Check that Docker is installed] ******************************************
ok: [speaker-node] => { "msg": "Docker version 29.3.1, build c2be9cc" }

TASK [Create the application directory] ****************************************
changed: [speaker-node]
TASK [Create the directory the database file lives in] *************************
changed: [speaker-node]
TASK [Write the environment file the container reads] **************************
changed: [speaker-node]
TASK [Start the application container] *****************************************
changed: [speaker-node]
FAILED - RETRYING: Wait for the application to report healthy (19 retries left).
TASK [Wait for the application to report healthy] ******************************
ok: [speaker-node]
TASK [Show the health-check result] ********************************************
ok: [speaker-node] => {
    "msg": "speaker-session-manager is UP on port 8080 (UP)"
}

PLAY RECAP *********************************************************************
speaker-node : ok=11   changed=4    unreachable=0    failed=0
```

Note the retry line: the playbook waited for the application to answer
rather than assuming the container was ready the moment it started.

## What was provisioned

```
$ ls -la ~/speaker-session-manager
total 16
drwxr-xr-x@   5 krishdhumal  staff   160 Oct 21 14:12 .
drwxr-x---+ 111 krishdhumal  staff  3552 Oct 21 14:11 ..
-rw-------@   1 krishdhumal  staff   178 Oct 21 14:11 app.env
drwxrwxrwx@   3 krishdhumal  staff    96 Oct 21 14:11 data
-rw-r--r--@   1 krishdhumal  staff    45 Oct 21 14:12 previous-image.txt

$ cat ~/speaker-session-manager/app.env
# Managed by Ansible — edit the playbook variables, not this file.
APP_ENV=ansible
PORT=8080
SEED_DEMO_DATA=true
DB_URL=jdbc:h2:file:/app/data/speakerdb;DB_CLOSE_ON_EXIT=FALSE
```

## Task 14 — idempotency

Running the identical playbook again changes nothing. Every task reports
`ok` instead of `changed`, and the recap line reads `changed=0`.

```
$ ansible-playbook playbook.yml        # second run, no arguments changed

TASK [Check that Docker is installed] ******************************************
ok: [speaker-node]
TASK [Create the application directory] ****************************************
ok: [speaker-node]
TASK [Create the directory the database file lives in] *************************
ok: [speaker-node]
TASK [Write the environment file the container reads] **************************
ok: [speaker-node]
TASK [Record the image the running container currently uses] *******************
ok: [speaker-node]
TASK [Check whether the wanted image is already running] ***********************
ok: [speaker-node]
TASK [Wait for the application to report healthy] ******************************
ok: [speaker-node]

PLAY RECAP *********************************************************************
speaker-node : ok=10   changed=0    unreachable=0    failed=0    skipped=3
```

`skipped=3` is the point: the container was not restarted, because the
wanted image was already running and the environment file had not changed.

### A bug this caught

The first attempt reported `changed=1` on the second run. The task that
records the previous image for rollback was rewriting the file every time.
That was two faults in one: it broke idempotency, and it made
`previous-image.txt` point at the *currently running* image — so a rollback
would have restored the very release it was supposed to undo. The task now
writes only when the running image genuinely differs from the wanted one.

## Health check

```
$ curl http://localhost:8080/actuator/health
{"status":"UP","groups":["liveness","readiness"]}
```

```
$ docker ps
NAMES                     IMAGE                                          STATUS
speaker-session-manager   localhost:5001/speaker-session-manager:1.0.0   Up About a minute (healthy)
speaker-app               localhost:5001/speaker-session-manager:1.0.0   Up 4 minutes (healthy)
registry                  registry:2                                     Up 4 minutes
```

## Task 14 — rollback and recovery

A deliberately broken release was built to make the failure genuine: it is
the working image with `-Dserver.port=9999`, so the application starts but
never answers on 8080.

### 1. Deploy the broken release — the play fails

```
$ ansible-playbook playbook.yml -e image_tag=1.0.1-broken

TASK [Start the application container] *****************************************
changed: [speaker-node]
FAILED - RETRYING: Wait for the application to report healthy (2 retries left).
FAILED - RETRYING: Wait for the application to report healthy (1 retries left).
FAILED - RETRYING: Wait for the application to report healthy (0 retries left).

fatal: [speaker-node]: FAILED! => {"attempts": 20, ... "msg": "Status code
was -1 and not [200]: Connection failure", "url":
"http://localhost:8080/actuator/health"}

PLAY RECAP *********************************************************************
speaker-node : ok=11   changed=2    unreachable=0    failed=1
```

The playbook refused to report success for a release that does not answer.

### 2. Roll back

```
$ ansible-playbook rollback.yml

TASK [Show what will be restored] **********************************************
ok: [speaker-node] => {
    "msg": "Rolling speaker-session-manager back to
            localhost:5001/speaker-session-manager:1.0.0"
}
TASK [Remove the bad container] ************************************************
changed: [speaker-node]
TASK [Start the previous image again] ******************************************
changed: [speaker-node]
TASK [Confirm the restored release is healthy] *********************************
ok: [speaker-node]
TASK [Report success] **********************************************************
ok: [speaker-node] => {
    "msg": "Rollback complete — localhost:5001/speaker-session-manager:1.0.0
            is UP on port 8080"
}

PLAY RECAP *********************************************************************
speaker-node : ok=8    changed=2    unreachable=0    failed=0
```

### 3. Confirm recovery

```
$ docker ps --format "{{.Names}}: {{.Image}}"
speaker-session-manager: localhost:5001/speaker-session-manager:1.0.0
speaker-app: localhost:5001/speaker-session-manager:1.0.0

$ curl http://localhost:8080/actuator/health
{"status":"UP","groups":["liveness","readiness"]}
```
