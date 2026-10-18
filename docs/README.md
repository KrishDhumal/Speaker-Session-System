# Deliverables index

Which document or file answers each of the thirteen tasks.

| Task | Deliverable | Where |
|---|---|---|
| 1 | Problem statement, stakeholders, objectives, constraints, MVP scope | [01-problem-and-scope.md](01-problem-and-scope.md) |
| 2 | Backlog, acceptance criteria, sprint plan, board, Definition of Done, lifecycle diagram | [02-agile-plan.md](02-agile-plan.md) |
| 3 | SRS summary, use-case and architecture diagrams, data model, endpoint list, local setup | [03-srs-and-architecture.md](03-srs-and-architecture.md) |
| 4 | Repository layout, README, .gitignore, issue and PR templates, branch policy | [04-git-workflow.md](04-git-workflow.md), [`.github/`](../.github) |
| 5 | Feature branch, pull request and merge commands | [04-git-workflow.md](04-git-workflow.md#task-5--a-feature-on-its-own-branch) |
| 6 | Second branch, conflict resolution, tagging; Jenkins freestyle build job | [04-git-workflow.md](04-git-workflow.md#task-6--a-second-branch-a-conflict-and-a-tag), [07-jenkins-setup.md](07-jenkins-setup.md) |
| 7 | Jenkinsfile with checkout, build, package, deploy; parameterised environment | [`Jenkinsfile`](../Jenkinsfile), [07-jenkins-setup.md](07-jenkins-setup.md) |
| 8 | Test plan, Selenium scripts, failure screenshots | [05-test-plan.md](05-test-plan.md), [`src/test/java/.../it/`](../src/test/java/com/college/speaker/it) |
| 9 | Selenium in Jenkins, failing tests block deployment, defect demonstration | [05-test-plan.md](05-test-plan.md#task-9--continuous-testing-in-jenkins) |
| 10 | Dockerfile, image build, container lifecycle commands | [06-docker-guide.md](06-docker-guide.md), [`Dockerfile`](../Dockerfile) |
| 11 | Versioned image, registry push, commit-to-container pipeline | [06-docker-guide.md](06-docker-guide.md#task-11--jenkins-to-docker-end-to-end), [`Jenkinsfile`](../Jenkinsfile) |
| 12 | Prerequisites specification, Ansible inventory and playbook | [08-ansible-guide.md](08-ansible-guide.md), [`ansible/`](../ansible) |
| 13 | Provisioning, idempotency evidence, health check, rollback | [08-ansible-guide.md](08-ansible-guide.md#task-13--idempotency), [`ansible/rollback.yml`](../ansible/rollback.yml) |
