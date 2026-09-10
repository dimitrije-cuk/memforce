# Commit message workflow

| Field | Value |
|---|---|
| Document title | MemForce — Commit message workflow |
| Document identifier | MF-PRC-001 |
| Version | 1.0 |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Governs | Step 3 of the change path in [MF-CMP-001, clause 5](configuration-management.md#5-configuration-change-control) |

Every commit in this repository names what kind of change it is. The commit history is the
project's change record — [MF-CMP-001, clause 6](configuration-management.md#6-configuration-status-accounting)
relies on it to answer what changed, when, and why — so the message format is enforced by a hook
rather than left to habit.

The hook has no dependencies and no runtime: it is a POSIX shell script that Git runs on every
commit.

## Setup

Run once after cloning:

```bat
setup-hooks.bat
```

It points `core.hooksPath` at `.githooks` for this repository. That **replaces** `.git/hooks`
rather than adding to it, so check that directory for existing hooks first — see
[git-core-hookspath.md](git-core-hookspath.md).

On macOS and Linux, configure the same thing directly and make sure the hook is executable:

```sh
git config core.hooksPath .githooks
git update-index --chmod=+x .githooks/commit-msg
```

The executable bit is stored in Git, so it only needs to be set once for everyone.

## The rule

```
type: subject
```

- `type` is required and must be one of `feat`, `fix`, `docs`, `style`, `refactor`, `perf`,
  `test`, `build`, `ci`, `chore`, `revert`.
- `subject` is required and must hold at least one non-whitespace character.
- An optional scope and breaking-change marker are accepted: `feat(api)!: drop v1`.
- Only the first non-comment line is checked. The hook is silent when a commit is accepted.
- Messages Git generates itself (`Merge …`, `Revert …`, `fixup!`, `squash!`) are skipped.

Accepted:

```
feat(auth): add login support
docs: state the schema migration rule
```

Rejected:

```
WIP
updated some files
```

## Choosing a type

| Type | Use for |
| --- | --- |
| `feat` | A capability a user can observe. |
| `fix` | A defect fix; name the anomaly if one is registered. |
| `docs` | Documentation only, including the documents under `docs/`. |
| `refactor` | A change to structure that leaves behaviour identical. |
| `perf` | A change made to meet or protect a performance requirement. |
| `test` | Adding or changing tests only. |
| `build` | Gradle, the wrapper, dependencies, or the SDK setup script. |
| `ci` | Automation that runs outside a developer's machine. |
| `style` | Formatting with no change of meaning. |
| `chore` | Repository housekeeping that fits nothing above. |
| `revert` | Undoing an earlier commit. |

A change that alters behaviour **and** its documentation is one commit, not two: MF-CMP-001,
clause 5.1 requires the documents to move with the change that makes them wrong.

## Files

| Path | Purpose |
| --- | --- |
| `.githooks/commit-msg` | Validates the commit message. |
| `setup-hooks.bat` | Points `core.hooksPath` at `.githooks` on Windows. |
| `test-hooks.bat` | Self-check for the hook, used when changing the rule. |
| `.gitattributes` | Keeps `.githooks/**` checked out with LF, so the shebang works on every platform. |

## Testing the hook

After changing the rule in `.githooks/commit-msg`, run the self-check from the repository root:

```bat
test-hooks.bat
```

It runs the hook against accepted and rejected sample messages and reports any that behave
unexpectedly. The hook is a shell script, so the script uses the `sh` that ships with Git for
Windows; on macOS and Linux run the hook against the same samples with `sh .githooks/commit-msg`.

## Bypass and removal

```sh
# Commit without running the hook.
git commit --no-verify

# Stop using the repository hooks.
git config --unset core.hooksPath
```

`--no-verify` exists for emergencies. A commit that used it still has to meet the rule before it
reaches `main`, because the message is part of the record the plan depends on; amend it rather
than leaving it.
