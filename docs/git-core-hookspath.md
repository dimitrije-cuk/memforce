# Git `core.hooksPath`

`core.hooksPath` tells Git where to find Git hooks. Requires Git 2.9 or newer.

```bash
# Set hooks directory for this repository.
git config core.hooksPath .githooks

# Set an absolute hooks directory for this repository.
git config core.hooksPath "D:/Projects/my-hooks"

# Set hooks directory for every repository of the current user.
git config --global core.hooksPath .githooks

# Show the configured value and where it comes from.
git config --show-origin --get core.hooksPath

# Remove the repository-level setting.
git config --unset core.hooksPath

# Remove the user-level setting.
git config --global --unset core.hooksPath
```

## Scope

- Without `--global`: current repository.
- With `--global`: all repositories for the current user. This overrides the hooks of every repository, including ones that ship their own, so prefer the repository-level setting.

## It replaces `.git/hooks`

Git looks in one hooks directory, not several. Once `core.hooksPath` is set, nothing in `.git/hooks` runs any more — the two are not merged.

Adopting this template in an existing repository therefore disables any hooks already installed under `.git/hooks`. Check that directory for non-sample files before running the setup script, and move anything worth keeping into `.githooks`.

## Relative paths

A relative path such as `.githooks` is resolved against the directory hooks run from: the top level of the working tree for a normal repository, or `$GIT_DIR` for a bare one. It is not resolved against the current directory.

## Conflicts with other tools

husky and similar tools configure the same key, and the last one to run wins. If a repository already uses one of them, adopting this template points Git away from their hooks, and reinstalling them later points Git away from `.githooks`.

## When a hook does not run

- Confirm the value with `git config --show-origin --get core.hooksPath`.
- On macOS and Linux, the hook file must be executable. See [commit-message-workflow.md](./commit-message-workflow.md).
- `git commit --no-verify` skips the hook.
