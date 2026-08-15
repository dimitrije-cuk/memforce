@echo off
git rev-parse --git-dir >NUL 2>&1 || (echo Not a Git repository. & exit /b 1)
git config core.hooksPath .githooks
echo Git hooks configured: .githooks