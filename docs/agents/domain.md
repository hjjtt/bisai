# Domain Docs

## Layout

Single-context. The repo has one `CONTEXT.md` at the root and one `docs/adr/` directory.

## Files

- `CONTEXT.md` — Project domain language, architecture overview, key conventions
- `docs/adr/` — Architecture Decision Records (ADRs) for past technical decisions

## Consumer Rules

1. **Always read `CONTEXT.md` first** before working on any task. It contains the project's domain language and architectural context.
2. **Check `docs/adr/`** for past decisions that may affect your approach. Do not contradict established ADRs without proposing an update.
3. **If `CONTEXT.md` is missing**, create it before starting work. Use the existing `CLAUDE.md` / `AGENTS.md` as a starting point.
4. **If `docs/adr/` is empty**, create ADRs as you make significant architectural decisions. Use the format `NNNN-title.md`.
