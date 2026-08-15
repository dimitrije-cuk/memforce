# Commit message workflow

A **language-independent Git repository template** that establishes a standardized commit-message workflow from the very first commit.

Copy the template files into an empty Git repository, then run the setup script for your platform to configure Git to use the included hooks.

This gives new projects a consistent commit-message workflow from the start.

> This is not the `commitlint` npm package. The template has no dependencies and no runtime.

## Setup

Windows:

```bat
setup-hooks.bat
```

macOS and Linux:

```sh
sh setup-hooks.sh
```

Both point `core.hooksPath` at `.githooks` for the current repository. This replaces `.git/hooks` rather than adding to it, so check that directory for existing hooks first. See [git-core-hookspath.md](./git-core-hookspath.md).

Commit the executable bit once so the hook also runs on macOS and Linux:

```sh
git update-index --chmod=+x .githooks/commit-msg
```

## Commit message rule

```
type: subject
```

- `type` is required and must be one of `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- `subject` is required.
- An optional scope and breaking-change marker are accepted: `feat(api)!: drop v1`.
- Only the first line is checked. The hook is silent when a commit is accepted.
- Messages Git generates itself (merge, revert, `fixup!`, `squash!`) are skipped.

Accepted:

```
feat(auth): add login support
```

Rejected:

```
WIP
```

## Files

| Path | Purpose |
| --- | --- |
| `.githooks/commit-msg` | Validates the commit message. |
| `setup-hooks.bat` | Setup for Windows. |
| `setup-hooks.sh` | Setup for macOS and Linux. |
| `test-hooks.bat` | Self-check for the hook on Windows, used when changing the rule. |
| `test-hooks.sh` | Self-check for the hook on macOS and Linux, used when changing the rule. |
| `.gitattributes` | Keeps the hook checked out with LF endings. |
| `docs/` | Background notes. |

## Testing the hook

Run the self-check from the repository root after changing the rule in `.githooks/commit-msg`.

Windows:

```bat
test-hooks.bat
```

macOS and Linux:

```sh
sh test-hooks.sh
```

The hook is a shell script, so `test-hooks.bat` needs the `sh` that ships with Git for Windows.

## Bypass and removal

```sh
# Commit without running the hook.
git commit --no-verify

# Stop using the hooks.
git config --unset core.hooksPath
```

The `--unset` above clears the repository-level setting. See [git-core-hookspath.md](./git-core-hookspath.md) for the user-level variant and for what to check when a hook does not run.

## Optional cleanup

Projects that keep only one platform's scripts can drop the matching line from `.gitattributes`:

- `*.sh text eol=lf` once the `.sh` scripts are gone.
- `*.bat text eol=crlf` once the `.bat` scripts are gone.

Leaving them in place is harmless. Keep `.githooks/** text eol=lf` either way.
