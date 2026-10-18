# Tasks 13–14 — Configuration Management and Provisioning

## Why Ansible rather than Puppet

Puppet needs an agent installed on every managed node and, in its usual setup, a
Puppet master to talk to. Ansible needs neither: it connects over plain SSH,
pushes the work, and disconnects. For a single target node and a laptop
demonstration that is far less to install, and the playbook reads top to bottom
as a list of steps.

Install it when you want to run the playbooks:

```bash
brew install ansible          # macOS
sudo apt install ansible      # Debian/Ubuntu
ansible --version
```

## Server prerequisites specification

What the target node must have before the application can run:

| Requirement | Detail | Handled by |
|---|---|---|
| Docker engine | Must be installed and running | Checked by the playbook; it fails fast with a clear message if missing |
| Application directory | `/opt/speaker-session-manager` | Created, mode 0755 |
| Data directory | `/opt/speaker-session-manager/data` | Created, writable by the container user |
| Environment file | `app.env`, rendered from a template | Created, mode 0600 |
| Port | `8080` reachable on the host | Published by the container |
| Service | Container named `speaker-session-manager`, `--restart unless-stopped` | Started by the playbook |
| Health endpoint | `/actuator/health` returning `UP` | Polled before the run is called a success |

## Files

```
ansible/
├── ansible.cfg            defaults so you can run `ansible-playbook playbook.yml`
├── inventory.ini          which hosts, and the variables for them
├── playbook.yml           provision + deploy
├── rollback.yml           return to the previous image
└── templates/
    └── app.env.j2         the environment file, rendered per host
```

### The inventory is the only place that knows about hosts

```ini
[speaker_servers]
speaker-node ansible_connection=local
```

To target a real VM instead, replace that one line:

```ini
speaker-node ansible_host=192.168.56.10 ansible_user=ubuntu
```

Nothing in the playbook changes.

## Running it

```bash
cd ansible
ansible-playbook playbook.yml              # provision and deploy
ansible-playbook playbook.yml --check      # dry run: changes nothing
ansible-playbook playbook.yml --tags deploy
ansible-playbook playbook.yml -v           # more detail
```

## Task 14 — idempotency

Idempotent means *running it twice changes nothing the second time*. Ansible
reports this in its summary line:

```
# First run
PLAY RECAP
speaker-node : ok=8  changed=5  unreachable=0  failed=0

# Second run, immediately after — no changes
PLAY RECAP
speaker-node : ok=8  changed=0  unreachable=0  failed=0
```

`changed=0` on the second run is the evidence. It works because the playbook
asks what is already true before acting:

- directories use `state: directory`, which does nothing if they already exist;
- the environment file is a template — Ansible compares the rendered content and
  only writes on a difference;
- before deploying, the playbook reads the image the running container was
  started from. The container is only replaced if the wanted image differs, or
  the environment file just changed.

That last check is what stops a redeploy from pointlessly restarting a healthy
container.

## Health check

The last task polls `/actuator/health` until it returns `UP`, retrying twenty
times at five-second intervals. If the application never comes up, the playbook
fails rather than reporting a successful deployment of a broken release.

## Rollback

`playbook.yml` writes the image it is replacing into
`/opt/speaker-session-manager/previous-image.txt`. `rollback.yml` reads it back:

```bash
ansible-playbook rollback.yml
```

It stops the bad container, starts the recorded previous image, and polls the
health endpoint again. If no previous image was recorded, it fails with a clear
message rather than guessing.

### Demonstrating recovery

```bash
# 1. deploy a known-good version
ansible-playbook playbook.yml -e image_tag=1.0.0

# 2. deploy a broken one
ansible-playbook playbook.yml -e image_tag=1.0.1-broken
#    the health check fails and the play stops

# 3. recover
ansible-playbook rollback.yml
#    1.0.0 is running again and healthy
```

## Puppet equivalent, for comparison

If the exercise requires Puppet instead, the same specification becomes a
manifest of resources — `package`, `file`, `service`, `exec` — applied with
`puppet apply manifests/site.pp`. Puppet's resource model is declarative in the
same way, but requires the agent (and usually a master) to be installed first,
which is the reason it was not chosen here.
