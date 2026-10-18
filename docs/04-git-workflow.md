# Tasks 4–6 — Git and GitHub Workflow

## Repository structure

```
.
├── .github/              issue templates, pull request template
├── ansible/              provisioning and rollback playbooks
├── docs/                 the written deliverables
├── src/main/java         application code
├── src/main/resources    templates, CSS, configuration
├── src/test/java         unit tests (*Test) and Selenium tests (*IT)
├── Dockerfile            multi-stage image build
├── Jenkinsfile           pipeline as code
├── docker-compose.yml    convenience wrapper for local runs
├── pom.xml               Maven build
└── README.md
```

## Branching policy

| Branch | Purpose | Merges into |
|---|---|---|
| `main` | Release-ready code only. Tagged for each version. | — |
| `develop` | Integration branch. All features land here first. | `main` |
| `feature/<short-name>` | One branch per user story. | `develop` |
| `bugfix/<short-name>` | A fix for a defect found in testing. | `develop` |

Rules:
- Never commit directly to `main`.
- One branch per story, named after the story, e.g. `feature/search-filters`.
- Open a pull request into `develop`; it needs a review comment before merging.
- Delete the branch after the merge.

## Commit message style

Imperative mood, under 72 characters, saying what the change does:

```
Add track filter to session search
Fix status badge colour for cancelled sessions
Enforce reviewer role on the approve transition
```

Not: `update`, `fix stuff`, `changes`.

## Publishing to GitHub

The repository is initialised locally. To publish it:

```bash
gh repo create speaker-session-manager --public --source=. --push
```

or, without the GitHub CLI, create an empty repository on github.com and then:

```bash
git remote add origin https://github.com/<your-username>/speaker-session-manager.git
git push -u origin main
git push origin develop
git push origin --tags
```

## Task 5 — a feature on its own branch

This is the sequence used for the first feature, and the one to repeat:

```bash
git checkout develop
git checkout -b feature/session-search

# ... edit files ...
git add src/main/java/com/college/speaker/repo/SpeakerSessionRepository.java
git status
git commit -m "Add combined keyword, status and track search"
git log --oneline --graph --all
git push -u origin feature/session-search
```

Then open the pull request:

```bash
gh pr create --base develop --head feature/session-search \
  --title "Add session search" \
  --body "Implements US-05, US-06 and US-07. One query backs all three filters."
```

Review the diff, leave a comment, then merge:

```bash
gh pr review --comment --body "Query handles null filters cleanly. Approved."
gh pr merge --merge --delete-branch
```

## Task 6 — a second branch, a conflict, and a tag

Create a conflict deliberately so you can show resolving one. Two branches edit
the same line of `README.md`:

```bash
git checkout develop
git checkout -b feature/readme-stack-table
# edit the same line in README.md, commit
git commit -am "Describe the stack in the README"

git checkout develop
git checkout -b bugfix/readme-wording
# edit that same line differently, commit
git commit -am "Reword the README introduction"

git checkout develop
git merge feature/readme-stack-table     # fast-forward, fine
git merge bugfix/readme-wording          # CONFLICT
```

Git stops and marks the file:

```
<<<<<<< HEAD
the version from feature/readme-stack-table
=======
the version from bugfix/readme-wording
>>>>>>> bugfix/readme-wording
```

Resolve it by editing the file so it contains the intended final text, removing
all three marker lines, then:

```bash
git add README.md
git commit -m "Merge bugfix/readme-wording, keeping both the table and the new wording"
```

Tag the release once `develop` is merged into `main`:

```bash
git checkout main
git merge develop
git tag -a v1.0.0 -m "MVP: create, search, role-based workflow, dashboard"
git push origin main --tags
```

## Useful inspection commands

```bash
git log --oneline --graph --all --decorate   # branch and merge history
git diff develop..feature/session-search     # what a branch changes
git show v1.0.0                              # what a tag points at
git branch -vv                               # local branches and tracking
```
