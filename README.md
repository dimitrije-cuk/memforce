# MemForce

Android application for capturing quiz questions, classifying them with tags, and finding them
again. Everything runs on the device against a local SQLite database: there is no server, no
network traffic, and the application declares no permissions at all.

Questions and tags are shared by everyone who signs in on the device. A whole topic can be
imported at once from a JSON file — typically one a chatbot filled in from the published
template — instead of being typed in question by question.

## Documentation

| Document | Answers |
| --- | --- |
| [docs/README.md](docs/README.md) | The documentation set and how it fits together |
| [Software Requirements Specification](docs/requirements.md) | What the product must do, and how each obligation is judged |
| [Software Design Description](docs/design.md) | How it is built, and why that way |
| [Verification and Validation Plan](docs/verification-and-validation.md) | How it is tested, and what is known to be wrong |
| [Configuration Management Plan](docs/configuration-management.md) | What is controlled, how it changes, how a release is proved |
| [Question import format](docs/question-import-format.md) | The JSON question-set file, its schema and template |
| [Development environment](docs/development-environment.md) | How to make a machine able to build this |

## Build and run

Requires JDK 17 or newer. To install the Android SDK packages the build needs and point the
project at them:

```powershell
.\setup-env.ps1
```

Then build and install:

```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
gradlew.bat testDebugUnitTest
```

The setup script only writes to the SDK directory and to `local.properties`; it changes no
environment variables and needs no administrator rights. To set the environment up by hand
instead, or to undo it, see [docs/development-environment.md](docs/development-environment.md).

Platform baseline: `minSdk` 24, `compileSdk` and `targetSdk` 34, Java 17, Gradle 9.4.1.

## Screens

| Screen | Purpose |
| --- | --- |
| Sign in | Signs in; an unknown user name is registered on first use |
| Main menu | Shows who is signed in; leads to questions and tags; signs out |
| Questions | Search by text and by tag; add, edit, delete; assign tags; import a question set |
| Tags | Search by name; add, edit, delete |

The application follows the device's light or dark appearance; there is no in-app switch.

## Searching

Search fields are trimmed and then passed to SQL `LIKE` unescaped, so the wildcards work as
written:

- `po%` starts with `po`
- `%ta` ends with `ta`
- `%sto%` contains `sto`
- `_br%` has `b` second and `r` third
- `%__a` at least three letters, ending in `a`

An empty field lists everything, matching ignores letter case, and the list narrows as you type.

## Data model

Four tables in `memforce.db`, in application-private storage. Questions and tags are global: no
content row carries a user, so everyone signed in on the device sees the same library.

```
users          (_id, name UNIQUE NOCASE, password_hash, salt)
questions      (_id, name, answer NULL)
tags           (_id, name UNIQUE NOCASE)
question_tags  (question_id, tag_id)   PK(question_id, tag_id), both ON DELETE CASCADE
```

Deleting a tag removes it from every question that carries it; deleting a question removes its
tag assignments and leaves the tags. Foreign keys are switched on for every connection, which
SQLite does not do by default.

Passwords are never stored: `password_hash` holds a PBKDF2 value (HMAC-SHA1, 100 000 iterations,
256-bit key) derived over the password and a 16-byte random per-account salt, and verification
compares in constant time.

The schema is drawn in [docs/database/database-erd.puml](docs/database/database-erd.puml), with
generated [PNG](docs/database/database-erd.png) and [SVG](docs/database/database-erd.svg).

## Importing question sets

Typing questions in one at a time doesn't scale for a whole topic. MemForce defines a JSON
**question set** format — with tags at both the set level (applied to every question) and the
question level (applied to just one) — that can be filled in from a template and then imported.
See [docs/question-import-format.md](docs/question-import-format.md) for the format, schema,
template and example.

**Questions → Import** picks such a file. It is validated as a whole first, so a file that breaks
the format is reported — the first ten problems and a count of any others — and nothing is
written. The counts to be applied are shown before anything is stored. Missing tags are created;
a question whose text already exists (ignoring case) gains the file's tags instead of being stored
a second time, and its answer is kept.

## First launch

Creating the database seeds a small demo library — 25 questions, 9 tags — so the screens have
something to show, plus two demo accounts (`ana` and `marko`). The demo *accounts* should not ship
in a release build; this is recorded as anomaly
[A-05](docs/verification-and-validation.md#92-open-anomalies) with the other known deviations.

## Contributing

Commit messages follow `type: subject`, enforced by the hook in `.githooks`. Run `setup-hooks.bat`
once after cloning. The full change path — impact on requirements, gates a change must pass, and
what must move with it — is in
[MF-CMP-001, clause 5](docs/configuration-management.md#5-configuration-change-control); the
message rule itself is in [docs/commit-message-workflow.md](docs/commit-message-workflow.md).

A change to behaviour includes the documentation that describes it. That is what keeps the
documents above worth reading.
