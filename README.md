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
| Main menu | Entry point to questions, categories, tags and decks |
| Questions | Search by text, category and tag; add, edit, delete |
| Categories | Search by name and tag; assign tags; add, edit, delete |
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

Questions, categories and tags are shared by every user. Decks belong to the user that created them
and are filtered by user id in every query.

```
users        (id, name, password_hash, salt)
tags         (id, name)
categories   (id, name)
questions    (id, name, answer, category_id -> categories, ON DELETE SET NULL)
decks        (id, name, user_id -> users, ON DELETE CASCADE)
question_tags  (question_id, tag_id)     both ON DELETE CASCADE
category_tags  (category_id, tag_id)     both ON DELETE CASCADE
deck_questions (deck_id, question_id)    both ON DELETE CASCADE
```

Deleting a tag removes it from the questions and categories that use it. Deleting a category leaves
its questions in place without a category. Deleting a question removes it from every deck.

Passwords are stored as PBKDF2 hashes with a per-user random salt.

## Repository

Commit messages follow `type: subject`, enforced by the hook in `.githooks`. Run `setup-hooks.bat`
once after cloning. See [docs/commit-message-workflow.md](docs/commit-message-workflow.md).
