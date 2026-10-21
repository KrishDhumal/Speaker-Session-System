# Deliverables index

Which document or file answers each of the fourteen project tasks.

| Task | Title | Deliverable | Where | Evidence |
|---|---|---|---|---|
| 1 | Problem Definition and Scope | Problem statement, stakeholders, objectives, constraints, MVP scope | [01-problem-and-scope.md](01-problem-and-scope.md) | — |
| 2 | Agile Planning and DevOps Workflow | Backlog, board, sprint plan, Definition of Done, lifecycle diagram | [02-agile-plan.md](02-agile-plan.md) | — |
| 3 | Requirements, Architecture, Technology | SRS, use-case and architecture diagrams, data model, working local setup | [03-srs-and-architecture.md](03-srs-and-architecture.md) | — |
| 4 | Git and GitHub Repository Initialization | README, .gitignore, folder structure, issue templates, branch policy | [04-git-workflow.md](04-git-workflow.md), [`.github/`](../.github) | **Repository URL** — push to GitHub |
| 5 | Feature Development with Branching | Feature branch, add/commit/push/pull, pull request, review, merge | [04-git-workflow.md](04-git-workflow.md#task-5--a-feature-on-its-own-branch) | **Pull request + review comments** — needs GitHub |
| 6 | MVP Completion and Git Collaboration | Functional MVP, second branch, resolved conflict, tag, updated backlog | [04-git-workflow.md](04-git-workflow.md#task-6--a-second-branch-a-conflict-and-a-tag) | — (done locally: conflict resolved, `v1.0.0` tagged) |
| 7 | Jenkins Installation and CI Job | Freestyle job, Maven build triggered by commit or polling, archived artefact | [07-jenkins-setup.md](07-jenkins-setup.md#task-7--a-freestyle-build-job) | ✅ [task7-8-10-jenkins.md](evidence/task7-8-10-jenkins.md) |
| 8 | Pipeline as Code and Server Deployment | Jenkinsfile with checkout/build/package/deploy, Tomcat deploy, parameterised setting | [`Jenkinsfile`](../Jenkinsfile), [07-jenkins-setup.md](07-jenkins-setup.md) | ✅ [task7-8-10-jenkins.md](evidence/task7-8-10-jenkins.md) |
| 9 | Selenium Test Design and Local Execution | Test plan, scripts with assertions, local report, failure screenshots | [05-test-plan.md](05-test-plan.md), [`src/test/java/.../it/`](../src/test/java/com/college/speaker/it) | — (20 tests passing locally) |
| 10 | Continuous Testing in Jenkins | Selenium in the pipeline, published report, failed tests block deployment, defect fixed | [05-test-plan.md](05-test-plan.md#task-10--continuous-testing-in-jenkins) | ✅ [task7-8-10-jenkins.md](evidence/task7-8-10-jenkins.md) |
| 11 | Docker Image and Container Lifecycle | Dockerfile, image build and tag, run/logs/stop/restart/remove | [06-docker-guide.md](06-docker-guide.md), [`Dockerfile`](../Dockerfile) | ✅ [task11-docker-lifecycle.md](evidence/task11-docker-lifecycle.md) |
| 12 | Jenkins-Docker Continuous Deployment | Versioned image, registry push, fresh container after tests pass | [06-docker-guide.md](06-docker-guide.md#task-12--jenkins-to-docker-end-to-end), [`Jenkinsfile`](../Jenkinsfile) | ✅ [task12-registry-and-cd.md](evidence/task12-registry-and-cd.md) |
| 13 | Configuration Management Script | Prerequisites specification, Ansible inventory and playbook, first execution log | [08-ansible-guide.md](08-ansible-guide.md), [`ansible/`](../ansible) | ✅ [task13-14-ansible.md](evidence/task13-14-ansible.md) |
| 14 | Automated Provisioning and Reliability | Provisioned node, idempotency evidence, health check, rollback demonstration | [08-ansible-guide.md](08-ansible-guide.md#task-14--idempotency), [`ansible/rollback.yml`](../ansible/rollback.yml) | ✅ [task13-14-ansible.md](evidence/task13-14-ansible.md) |

## Execution evidence

Tasks 7, 8 and 10–14 ask for proof that a tool actually *ran*. Those logs were
captured from real runs on this machine and live in
[`docs/evidence/`](evidence):

| File | Covers |
|---|---|
| [task7-8-10-jenkins.md](evidence/task7-8-10-jenkins.md) | Freestyle job, pipeline job, the caught defect, registry push |
| [task11-docker-lifecycle.md](evidence/task11-docker-lifecycle.md) | Image build, every lifecycle command, volume persistence |
| [task12-registry-and-cd.md](evidence/task12-registry-and-cd.md) | Registry push, deploy from a pull, pipeline mapping |
| [task13-14-ansible.md](evidence/task13-14-ansible.md) | First run, `changed=0` rerun, health check, rollback |

## The one remaining gap

Tasks 4 and 5 ask for a **repository URL** and a **pull request with review
comments**. The Git history is complete locally — feature branches, a resolved
merge conflict and the `v1.0.0` tag — but the repository has no GitHub remote,
so neither of those two artefacts exists yet. Publishing it is a decision for
the repository owner:

```bash
gh repo create speaker-session-manager --public --source=. --push --remote=origin
git push origin develop --tags
```

Once pushed, open a pull request from a feature branch into `develop`, leave a
review comment and merge it — that completes Task 5.
