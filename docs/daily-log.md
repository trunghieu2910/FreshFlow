# FreshFlow Daily Log

This log records planned work, actual outcomes, evidence, blockers and next actions. Each entry should reference a backlog task and should remain concise enough to review at the end of the day.

## Daily log format

| Field | Required content |
|---|---|
| Date | Local working date, using `YYYY-MM-DD` |
| Task | Backlog task ID and task name |
| Goal | Intended outcome for the session |
| Completed | Work that was actually finished |
| Evidence | Commands, screenshots, links, test results or commit |
| Blockers | Anything preventing completion; write `None` when clear |
| Next action | The next concrete step |

## Entry — FF-01-05-2

**Date:** `2026-08-22`
**Task:** `FF-01-05-2 — Set up issue/backlog workflow`
**Priority:** `Must`
**Area:** `Git`

### Goal

Set up a repeatable GitHub workflow so every future FreshFlow task has acceptance criteria, verification evidence, Definition of Done and a completion report.

### Completed

- Created a task issue template at `.github/ISSUE_TEMPLATE/01-task.md`.
- Created a bug report template at `.github/ISSUE_TEMPLATE/02-bug-report.md`.
- Created `.github/ISSUE_TEMPLATE/config.yml` with blank issues disabled and documentation links.
- Created `.github/PULL_REQUEST_TEMPLATE.md` with acceptance, verification, scope, security and documentation checklists.
- Prepared this daily log structure at `docs/daily-log.md`.

### Evidence

The following local checks were completed:

```text
task template: OK
bug template: OK
issue config: OK
PR template: OK
```

The issue configuration uses standard YAML indentation. The pull request template is located directly under `.github`, while issue templates are located under `.github/ISSUE_TEMPLATE`.

### Blockers

GitHub CLI is not installed in the local Git Bash environment. Labels and issue/PR sample verification will therefore be completed through the GitHub web interface or after installing an authenticated GitHub CLI.

### Next action

Create or verify the `Must`, `Should` and `Stretch` labels in the `trunghieu2910/FreshFlow` repository, then create one issue using the task template and prepare a draft PR using the pull request template.

## Reusable entry template

### Entry — YYYY-MM-DD

**Task:** `FF-XX-XX-X — Task name`
**Priority:** `Must` / `Should` / `Stretch`
**Area:** `Backend` / `Database` / `Infrastructure` / `Web` / `Android` / `Git` / `Documentation` / `Quality`

#### Goal

<!-- State the intended outcome. -->

#### Completed

<!-- Record only work actually completed. -->

#### Evidence

```text
<!-- Commands, results, links or commit SHA. -->
```

#### Blockers

<!-- Write None when there is no blocker. -->

#### Next action

<!-- State one or more concrete next steps. -->
## Entry — FF-01-05-2 verification evidence

### Goal

Verify the GitHub issue workflow through a real temporary issue and priority labels.

### Completed

- Created the `Must`, `Should` and `Stretch` labels on GitHub.
- Created issue [#1 — Verify FreshFlow issue workflow](https://github.com/trunghieu2910/FreshFlow/issues/1 ) from the FreshFlow task template.
- Confirmed that the issue contains task summary, scope, acceptance criteria, verification plan and Definition of Done.
- Confirmed that issue #1 has the `Must` label.

### Evidence

- Issue: https://github.com/trunghieu2910/FreshFlow/issues/1
- Workflow commit on main: `90007e4`
- GitHub repository: https://github.com/trunghieu2910/FreshFlow

### Blockers

None.

### Next action

Create a draft pull request from `chore/verify-github-workflow` to verify automatic loading of `.github/PULL_REQUEST_TEMPLATE.md`.
