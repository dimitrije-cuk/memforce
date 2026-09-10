# MemForce

Android app for entering and searching quiz questions, and for building personal decks from them.
Everything runs on the device against a local SQLite database; there is no server.

## Build and run

Requires JDK 17 or newer and an Android SDK with platform 34.

```bat
gradlew.bat assembleDebug
gradlew.bat installDebug
```

`local.properties` must point at the SDK, for example `sdk.dir=C\:\\Users\\me\\AppData\\Local\\Android\\Sdk`.

## Screens

| Screen | Purpose |
| --- | --- |
| Login | Signs in; unknown users are registered on first use |
| Main menu | Entry point to questions, tags and decks |
| Questions | Search by text and tag; assign tags; add, edit, delete; import a question set file |
| Tags | Search by name; add, edit, delete |
| My decks | Search by name; pick questions; add, edit, delete |

## Searching

Search fields are passed to SQL `LIKE` unchanged, so the wildcards work as written:

- `po%` starts with `po`
- `%ta` ends with `ta`
- `%sto%` contains `sto`
- `_br%` has `b` second and `r` third
- `%__a` at least three letters, ending in `a`

An empty field lists everything.

## Data model

Questions and tags are shared by every user. Decks belong to the user that created them
and are filtered by user id in every query.

```
users        (id, name, password_hash, salt)
tags         (id, name)
questions    (id, name, answer)
decks        (id, name, user_id -> users, ON DELETE CASCADE)
question_tags  (question_id, tag_id)     both ON DELETE CASCADE
deck_questions (deck_id, question_id)    both ON DELETE CASCADE
```

Deleting a tag removes it from the questions that use it. Deleting a question removes it from
every deck.

Passwords are stored as PBKDF2 hashes with a per-user random salt.

## Importing question sets

Typing in questions one at a time doesn't scale for adding a whole topic at once. MemForce defines
a JSON **question set** format — with tags at both the set level (applied to every question) and
the individual-question level (applied to just one) — that a user can have an LLM chatbot fill in
from a template and then import. See [docs/question-import-format.md](docs/question-import-format.md)
for the format, schema, template, and example.

**Questions → Import** picks such a file and imports it. The file is validated as a whole first,
so a file that breaks the format is reported and nothing is written. Missing tags are created,
and a question whose text already exists (ignoring case) gains the file's tags instead of being
stored a second time; its answer is kept.

## Repository

Commit messages follow `type: subject`, enforced by the hook in `.githooks`. Run `setup-hooks.bat`
once after cloning. See [docs/commit-message-workflow.md](docs/commit-message-workflow.md).
