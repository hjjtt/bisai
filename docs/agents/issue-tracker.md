# Issue Tracker: GitHub

This repo uses GitHub Issues for tracking work.

## CLI

Use `gh` CLI for all issue operations:
- `gh issue create` — create new issue
- `gh issue list` — list issues
- `gh issue view <number>` — view issue details
- `gh issue edit <number> --add-label <label>` — add labels
- `gh issue close <number>` — close issue

## Repo

- **Remote**: `https://github.com/hjjtt/bisai.git`
- **GitHub URL**: `https://github.com/hjjtt/bisai`

## Workflow

1. Create issues with descriptive titles and clear body text
2. Apply triage labels immediately (see `triage-labels.md`)
3. Link related PRs to issues using `Fixes #<number>` in commit messages
4. Close issues when merged PR is deployed
