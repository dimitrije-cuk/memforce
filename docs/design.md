# MemForce — Software Design Description

| Field | Value |
|---|---|
| Document title | MemForce — Software Design Description |
| Document identifier | MF-SDD-001 |
| Version | 1.0 |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Subject | MemForce, version 1.0 (`versionCode` 1, `versionName` "1.0"), package `com.memforce` |
| Information item | Software Design Description (SDD) |
| Conforms to | IEEE Std 1016-2009, clauses 4 and 5 |
| Realises | [MF-SRS-001](requirements.md) |
| Verified by | [MF-VVP-001](verification-and-validation.md) |

## Conformance and viewpoint selection

IEEE Std 1016-2009 requires an SDD to identify its design stakeholders and their concerns, and to
present **design views** built from **design viewpoints** that address those concerns. The
viewpoints selected here are those that answer a concern of a stakeholder in
[clause 2](#2-design-stakeholders-and-concerns); the remainder of the catalogue in clause 5 of the
standard is not used, and the reason is stated below.

| Clause 5 viewpoint | Used | View / reason |
|---|---|---|
| Context | Yes | [3.1](#31-context-view) — the product boundary and its four external interfaces. |
| Composition | Yes | [3.2](#32-composition-view) — packages and their responsibilities. |
| Logical | Yes | [3.3](#33-logical-view) — domain entities and the classes that carry them. |
| Dependency | Yes | [3.4](#34-dependency-view) — the layering rule and what enforces it. |
| Information | Yes | [3.5](#35-information-view) — the persistent schema, integrity rules and seeded data. |
| Interface | Yes | [3.6](#36-interface-view) — the internal service interfaces and the two external ones. |
| Interaction | Yes | [3.7](#37-interaction-view) — sign-in, search and import, end to end. |
| State dynamics | Yes | [3.8](#38-state-dynamics-view) — session state and the import state machine. |
| Algorithm | Yes | [3.9](#39-algorithm-view) — validation, merge and pattern construction. |
| Resource | Yes | [3.10](#310-resource-view) — threads, memory bounds and database connections. |
| Patterns use | Yes | [3.11](#311-patterns-use-view) — the four recurring structures. |
| Structure | No | The product has no object structure beyond the composition of [3.2](#32-composition-view): there is a single deployable, no plug-in mechanism and no runtime object graph that outlives an activity. A separate structure view would restate composition. |

Design rationale is carried in [clause 4](#4-design-rationale); traceability to requirements in
[clause 5](#5-traceability); and the differences between this design and the requirements it
realises in [clause 6](#6-known-deviations-and-design-debt).

---

## 1 Identification

This document describes the design of MemForce version 1.0 as built: an Android application,
written in Java, that stores quiz questions and their tags in a SQLite database on the device.

It is written for the engineers who maintain and extend MemForce, for whoever verifies it, and
for a reviewer checking that the requirements of [MF-SRS-001](requirements.md) have been realised
in a way that can be maintained.

Where this document and the source disagree, the source is what runs and this document is a
defect. Where this document and [MF-SRS-001](requirements.md) disagree, the specification wins
and the design is a defect — except where [clause 6](#6-known-deviations-and-design-debt) records
the difference knowingly.

**Design language.** Views are expressed in prose, tables, ASCII structure and sequence diagrams,
and SQL data-definition statements. No modelling tool is required to read or to update them,
which is what keeps them current in a repository-based workflow.

---

## 2 Design stakeholders and concerns

| Stakeholder | Concern | Addressed by |
|---|---|---|
| Maintainer | Where does a change go, and what may it depend on? | [3.2](#32-composition-view), [3.4](#34-dependency-view), [4](#4-design-rationale) |
| Maintainer | What is the shape of the stored data, and what does the database guarantee on its own? | [3.5](#35-information-view) |
| Verifier | Which design element realises which requirement, and where is the evidence taken? | [5](#5-traceability), [MF-VVP-001](verification-and-validation.md) |
| Verifier | What is known to differ from the specification today? | [6](#6-known-deviations-and-design-debt) |
| Integrator writing question-set files | What exactly does the importer accept, and what does it do with a file? | [3.7.3](#373-question-set-import), [3.9](#39-algorithm-view), [MF-IFS-001](question-import-format.md) |
| Security reviewer | How is a credential stored and compared, and what leaves the device? | [3.5.4](#354-credential-storage), [3.9.4](#394-credential-derivation), [6](#6-known-deviations-and-design-debt) |
| Product owner | Does the delivered structure still permit the deferred capabilities of MF-SRS-001, 1.8? | [4.2](#42-decisions-that-shape-the-structure) |

---

## 3 Design views

### 3.1 Context view

*Concern addressed:* what is inside MemForce, what is outside, and across which interfaces they
meet.

```text
                        ┌──────────────────────────────────────────┐
      touch input       │                MemForce                  │
  User ───────────────▶ │  (single Android application package,    │
      screen output     │   process-local, no background service)  │
       ◀─────────────── │                                          │
                        └───┬───────────────┬──────────────────┬───┘
                            │               │                  │
              SQLiteOpenHelper       SharedPreferences   ContentResolver +
                (memforce.db)        (memforce_session)  OpenDocument picker
                            │               │                  │
                            ▼               ▼                  ▼
                   ┌────────────────┐ ┌─────────────┐ ┌──────────────────┐
                   │ SQLite database│ │ Preferences │ │ Question-set file│
                   │ private storage│ │ private     │ │ user-chosen,     │
                   │ questions/tags/│ │ signed-in   │ │ read-only, JSON  │
                   │ accounts       │ │ identity    │ │                  │
                   └────────────────┘ └─────────────┘ └──────────────────┘
```

| External entity | Direction | Content | Realises |
|---|---|---|---|
| User | in/out | Text entry, list selection, confirmation dialogs | REQ-EXT-10, REQ-EXT-30 |
| SQLite database `memforce.db` | in/out | All questions, tags, assignments and accounts | REQ-EXT-60, REQ-DB-10 |
| Preferences `memforce_session` | in/out | Signed-in account identifier and name only | REQ-AUTH-70, REQ-SEC-50 |
| Question-set file | in | One JSON document per import, read once, never written | REQ-EXT-70, REQ-IMP-10 |

There is no fifth arrow: the application holds no permission, opens no socket, starts no service,
and registers no receiver (REQ-EXT-50, REQ-CON-50).

### 3.2 Composition view

*Concern addressed:* where a change goes.

All code is under `app/src/main/java/com/memforce/`.

| Package | Element | Responsibility | Realises |
|---|---|---|---|
| `db` | `MemForceDbHelper` | The single `SQLiteOpenHelper`. Creates the schema, switches foreign keys on for every connection, seeds a new database. | REQ-DB-10, REQ-DB-50 |
| `db` | `DbContract` | Table and column names as constants; the one place a name is spelled. | REQ-DB-20 |
| `db` | `DatabaseSeeder` | Package-private. Populates a newly created database from `assets/seed/memforce_seed.sql` and two accounts. | [3.5.5](#355-seeded-data) |
| `db` | `SearchPatterns` | Turns a user's typed criterion into a `LIKE` argument. | REQ-SRCH-40, REQ-EXT-40 |
| `data` | `QuestionDao` | Question queries and writes, including tag assignment. | REQ-QST-10…70, REQ-SRCH-10…30 |
| `data` | `TagDao` | Tag queries and writes. | REQ-TAG-10…50, REQ-SRCH-50 |
| `data` | `UserDao` | `authenticateOrRegister` — the whole of sign-in as one database operation. | REQ-AUTH-20…40 |
| `data` | `QuestionSetImporter` | Two-phase import: `plan()` counts, `apply()` writes in one transaction. Carries `ImportPlan` and `ImportResult`. | REQ-IMP-40, REQ-IMP-50…80, REQ-IMP-100 |
| `importer` | `QuestionSetParser` | Parses and validates a question-set document; collects every violation before failing. | REQ-IMP-20, REQ-IMP-30, REQ-STD-30 |
| `importer` | `MergedQuestion` | Folds repeated questions within one file and unites their tags. | REQ-IMP-70 |
| `importer` | `QuestionSet`, `QuestionSetEntry`, `ValidationError`, `QuestionSetFormatException` | The parsed document, one entry, one violation, and the failure that carries them all. | REQ-IMP-20 |
| `model` | `Question`, `Tag`, `User`, `Named` | Immutable row carriers. `Named` is the `getId()`/`getName()` pair that the shared list adapter binds to. | [3.3](#33-logical-view) |
| `security` | `PasswordHasher` | PBKDF2 derivation, Base64 encoding, constant-time comparison. | REQ-SEC-10, REQ-SEC-20 |
| `session` | `Session` | Reads and writes the signed-in identity in preferences. | REQ-AUTH-70, REQ-AUTH-80, REQ-SEC-50 |
| `ui` | `MainActivity` | Greeting, the two area buttons, sign-out. | REQ-EXT-20, REQ-USE-10 |
| `ui.login` | `LoginActivity` | Sign-in form; derivation on a background executor; skips itself when a session exists. | REQ-AUTH-10…80, REQ-PERF-40 |
| `ui.question` | `QuestionListActivity` | Question list, text search, tag filter, delete, entry to the editor and to import. | REQ-QST-40, REQ-QST-50, REQ-SRCH-10…40, REQ-SRCH-80 |
| `ui.question` | `QuestionEditActivity` | Create and edit a question, including its tag selection. | REQ-QST-10, REQ-QST-20, REQ-QST-30, REQ-QST-70 |
| `ui.question` | `QuestionSetImportFlow` | The import interaction: pick, read, validate, plan, confirm, apply, report. | REQ-IMP-10…100 |
| `ui.tag` | `TagListActivity`, `TagEditActivity` | Tag list with search and delete; tag create and rename. | REQ-TAG-10…50 |
| `ui.common` | `EntityAdapter<T extends Named>` | One `RecyclerView` adapter for both lists: title, optional subtitle, click, delete. | REQ-QST-50, REQ-TAG-50 |
| `ui.common` | `FilterSpinner<T extends Named>` | A spinner whose first entry means "no filter". | REQ-SRCH-20, REQ-SRCH-40 |
| `ui.common` | `TagPicker` | Multi-choice tag dialog used by the question editor. | REQ-QST-70 |

Resources of note: `res/values/themes.xml` and `res/values-night/themes.xml` hold the light and
dark variants of `Theme.MemForce` (REQ-USE-60); `res/values/strings.xml` holds every user-facing
message, including the wildcard help of REQ-USE-70 and the import messages of REQ-IMP-20 to
REQ-IMP-100.

### 3.3 Logical view

*Concern addressed:* the entities the product reasons about, independent of storage.

```text
   User                Question  ────<  assignment  >────  Tag
   ├ id                ├ id                                ├ id
   ├ name              ├ name (question text)              └ name
   ├ passwordHash      └ answer (nullable)
   └ salt

   QuestionSet                     MergedQuestion
   ├ formatVersion                 ├ question text
   ├ name, description             ├ answer (first non-null wins)
   ├ tags (set level)              └ tags (set level ∪ entry level, deduplicated)
   └ entries: QuestionSetEntry[]
```

`Question`, `Tag` and `User` are row carriers: they hold what a query returned and nothing more,
with no behaviour and no reference to the database. `Question` additionally carries the joined tag
names as a display string, because the list shows them and a second query per row would not meet
REQ-PERF-10.

`Named` (`getId()`, `getName()`) is the only abstraction over the model. It exists so that
`EntityAdapter` and `FilterSpinner` are written once and used for both questions and tags.

The importer's model is deliberately separate from the persistent model: `QuestionSet` and
`QuestionSetEntry` describe *a file*, `MergedQuestion` describes *the intent* to store something,
and neither knows what is already stored. That separation is what allows the whole of parsing and
merging to run on a plain JVM under unit test (REQ-POR-40).

### 3.4 Dependency view

*Concern addressed:* what may depend on what, so that the layering does not erode.

```text
   ui.login   ui   ui.question   ui.tag   ui.common          ← Android UI
        │      │        │          │          │
        └──────┴────────┴────┬─────┴──────────┘
                             ▼
                  data (DAOs, QuestionSetImporter)            ← application services
                             │
              ┌──────────────┼───────────────┐
              ▼              ▼               ▼
             db          security         session             ← platform adapters
              │
              ▼
        Android SQLite

                  importer  ────────────────────────────────  ← pure Java, no Android
                     ▲
                     └── used by data.QuestionSetImporter and by JVM unit tests
```

Rules that hold in the delivered code:

1. **The user interface never issues SQL.** Every statement is inside `data` or `db`; activities
   call DAO methods only.
2. **`importer` depends on no Android type.** It uses `org.json` only, which is present on the
   platform at run time and supplied as a test dependency (`org.json:json`) on the JVM. This is
   what makes REQ-POR-40 verifiable rather than aspirational.
3. **`db` does not depend on `data`.** The contract and the helper know nothing about the DAOs
   that use them.
4. **No element depends on `ui`.** There is no callback from a DAO into an activity; results are
   returned, not published.
5. **Third-party dependencies are declared once**, in `gradle/libs.versions.toml`, and are limited
   to AndroidX AppCompat, ConstraintLayout, RecyclerView, Material Components, and — for tests
   only — JUnit and `org.json` (REQ-CON-30, REQ-CON-40).

### 3.5 Information view

*Concern addressed:* the persistent data, its constraints, and what the database enforces without
the application's help.

#### 3.5.1 Schema

Database file `memforce.db`, schema version 1, created by `MemForceDbHelper.onCreate`:

```sql
CREATE TABLE users (
    _id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    salt          TEXT NOT NULL);

CREATE TABLE tags (
    _id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE COLLATE NOCASE);

CREATE TABLE questions (
    _id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name   TEXT NOT NULL,
    answer TEXT);

CREATE TABLE question_tags (
    question_id INTEGER NOT NULL REFERENCES questions(_id) ON DELETE CASCADE,
    tag_id      INTEGER NOT NULL REFERENCES tags(_id)      ON DELETE CASCADE,
    PRIMARY KEY (question_id, tag_id));

CREATE INDEX idx_question_tags_tag ON question_tags(tag_id);
```

The rendered entity-relationship diagram is kept beside this document:
[database-erd.puml](database/database-erd.puml), with generated
[PNG](database/database-erd.png) and [SVG](database/database-erd.svg).

| Design choice | Why | Realises |
|---|---|---|
| `_id` rather than `id` | `android.provider.BaseColumns` names the key `_id`; adapters and `CursorAdapter`-style code expect it. | REQ-DB-30 |
| `AUTOINCREMENT` | Guarantees a deleted key is never handed out again, which `INTEGER PRIMARY KEY` alone does not. | REQ-DB-30 |
| `COLLATE NOCASE` on `users.name` and `tags.name` | Makes uniqueness case-insensitive **in the database**, so the rule holds even if a future caller forgets it. | REQ-DB-80, REQ-AUTH-60, REQ-TAG-20 |
| Composite primary key on `question_tags` | A tag applies to a question at most once, enforced by the schema rather than by the caller. | REQ-DB-40, REQ-QST-70 |
| `ON DELETE CASCADE` on both foreign keys | Deleting a question or a tag removes its assignments and nothing else. | REQ-DB-60, REQ-DB-70 |
| `idx_question_tags_tag` | The tag filter of REQ-SRCH-20 selects by `tag_id`; without the index that is a scan of the assignment table at every keystroke. | REQ-PERF-10 |
| `questions.answer` nullable | A question may be captured before its answer is known. | REQ-QST-10 |

#### 3.5.2 Integrity enforcement

SQLite disables foreign keys by default, per connection. `MemForceDbHelper.onConfigure` calls
`setForeignKeyConstraintsEnabled(true)`, which the platform applies to every connection the helper
opens; without it the two `ON DELETE CASCADE` clauses above would be inert and deleting a tag
would leave orphaned assignment rows (REQ-DB-50, decision D-14).

#### 3.5.3 Ownership

No content table carries an account key: questions, tags and assignments are global, which is what
REQ-QST-60 and REQ-TAG-60 require. `users` therefore stands alone in the schema and participates
in no relationship — it backs sign-in only. Introducing the personal collections deferred in
MF-SRS-001, 1.8 means adding an owning key to a new table, not changing these four.

#### 3.5.4 Credential storage

`users.password_hash` and `users.salt` hold Base64 (`NO_WRAP`) text. The salt is 16 bytes from
`SecureRandom`; the stored value is the 256-bit PBKDF2 output described in
[3.9.4](#394-credential-derivation). No column holds a password, and no other table references
`users` (REQ-SEC-10, REQ-SEC-50).

#### 3.5.5 Seeded data

A newly created database is populated by `DatabaseSeeder`, so that a first launch shows a working
library rather than three empty screens:

- two accounts, `ana` and `marko`, inserted through `PasswordHasher` so that the stored values are
  derived on the device like any other account;
- `app/src/main/assets/seed/memforce_seed.sql`, executed statement by statement: 9 tags, 25
  questions and 53 assignments.

Seeding runs inside `onCreate` only; it never runs against an existing database. The demo accounts
are a known deviation — see [A-05](#6-known-deviations-and-design-debt).

### 3.6 Interface view

*Concern addressed:* the operations each element offers, and their contract.

#### 3.6.1 Data services

| Operation | Contract | Realises |
|---|---|---|
| `QuestionDao.search(namePattern, tagId)` | Returns questions whose `name LIKE namePattern`, restricted to those carrying `tagId` when it is given, each with its tag names joined by `", "`, ordered by `name COLLATE NOCASE ASC`. | REQ-SRCH-10…40, REQ-QST-50 |
| `QuestionDao.findById(id)` | The single question, with tag names, or `null`. | REQ-QST-30 |
| `QuestionDao.findIdByName(name)` | The identifier of the question whose text equals `name` ignoring case, or `null`. The importer's duplicate test. | REQ-IMP-60 |
| `QuestionDao.tagIdsOf(questionId)` | The tag identifiers assigned to a question; used to pre-select the tag picker. | REQ-QST-70 |
| `QuestionDao.insert(name, answer, tagIds)` | Inserts a question and its assignments in one transaction; returns the new identifier or `-1`. | REQ-QST-10, REQ-REL-20 |
| `QuestionDao.update(id, name, answer, tagIds)` | Updates the row and replaces its assignments in one transaction. | REQ-QST-30 |
| `QuestionDao.addTags(questionId, tagIds)` | Adds assignments, ignoring those that exist. Used by import merge. | REQ-IMP-60 |
| `QuestionDao.fillMissingAnswer(id, answer)` | Sets the answer **only** where the stored answer is null or blank; never overwrites. | REQ-IMP-60 |
| `QuestionDao.delete(id)` | Deletes the question; assignments cascade. | REQ-QST-40, REQ-DB-70 |
| `TagDao.search(namePattern)` | Tags matching the pattern, ordered by `name COLLATE NOCASE ASC`. | REQ-SRCH-50, REQ-TAG-50 |
| `TagDao.findByName(name)` | Case-insensitive lookup, by column collation. | REQ-IMP-50 |
| `TagDao.insert(name)` | Returns `-1` when the unique constraint rejects the name, rather than throwing. | REQ-TAG-10, REQ-TAG-20 |
| `TagDao.update(id, name)` | Returns `false` on a constraint violation or when no row changed. | REQ-TAG-30 |
| `TagDao.delete(id)` | Deletes the tag; assignments cascade; questions survive. | REQ-TAG-40, REQ-DB-60 |
| `UserDao.authenticateOrRegister(name, password)` | Inside one transaction: existing name and matching password → that `User`; existing name and wrong password → `null`; unknown name → a newly created `User`. | REQ-AUTH-20…40 |
| `QuestionSetImporter.plan(set)` | Read-only. Counts questions to create, questions to merge, tags to create. | REQ-IMP-40 |
| `QuestionSetImporter.apply(set)` | One transaction over the whole set; returns the counts actually applied. | REQ-IMP-50…80, REQ-IMP-100 |
| `PasswordHasher.newSalt()` / `hash(password, salt)` / `matches(password, salt, expectedHash)` | Generates a 16-byte Base64 salt; derives the value; compares in constant time. Clears the key material afterwards. | REQ-SEC-10, REQ-SEC-20 |
| `Session.signIn(user)` / `isSignedIn()` / `signOut()` | Stores, reports and clears the identifier and name in `memforce_session`. | REQ-AUTH-70, REQ-AUTH-80, REQ-SEC-50 |
| `SearchPatterns.like(input)` | `null` or blank → `"%"`; otherwise the trimmed input, unescaped. | REQ-SRCH-40, REQ-EXT-40 |

#### 3.6.2 External interfaces

| Interface | Realisation | Realises |
|---|---|---|
| Document selection | `ActivityResultContracts.OpenDocument`, MIME filter `application/json`, `text/plain`, `application/octet-stream`; the stream is opened through `ContentResolver.openInputStream` and closed after one read. | REQ-EXT-70, REQ-IMP-10 |
| Question-set document | [MF-IFS-001](question-import-format.md) and its JSON Schema. `formatVersion` `"1.0"` only. | REQ-IMP-30, REQ-STD-30 |
| Navigation | `LoginActivity` is the launcher; `MainActivity` is `exported=false`; each of the four list and editor activities declares `parentActivityName`, so the system Back button returns to the screen that opened it. Both themes are `NoActionBar` and no toolbar is installed, so navigation is by the system Back button rather than an on-screen arrow. | REQ-EXT-20 |

### 3.7 Interaction view

*Concern addressed:* how elements collaborate for the three interactions that cross layers.

#### 3.7.1 Sign-in

```text
User        LoginActivity        Executor          UserDao          PasswordHasher   Session
 │ credentials  │                   │                 │                    │            │
 ├─────────────▶│ validate not empty (REQ-AUTH-50)     │                    │            │
 │              ├──────────────────▶│ submit           │                    │            │
 │              │                   ├────────────────▶ │ authenticateOrRegister          │
 │              │                   │                  ├───────────────────▶│ derive/compare
 │              │                   │                  │◀───────────────────┤            │
 │              │◀──────────────────┤ User or null     │                    │            │
 │              ├─────────────────────────────────────────────────────────────────────▶ │ signIn
 │◀─────────────┤ MainActivity, or wrong-password message on the field                  │
```

Derivation runs on the executor because 100 000 PBKDF2 iterations take long enough to be visible
on the reference device (REQ-PERF-40, decision D-13).

#### 3.7.2 Search

```text
User        QuestionListActivity      SearchPatterns        QuestionDao
 │ keystroke      │                        │                     │
 ├───────────────▶│ afterTextChanged → reload()                  │
 │                ├───────────────────────▶│ like(text) → "%…%"  │
 │                ├─────────────────────────────────────────────▶│ search(pattern, tagId)
 │                │◀─────────────────────────────────────────────┤ questions + joined tags
 │◀───────────────┤ adapter.submit(...) or the no-results message │
```

The same path serves the tag filter: choosing an entry in `FilterSpinner` calls the same
`reload()`, with the spinner's first entry meaning "no tag" (REQ-SRCH-20, REQ-SRCH-40).

#### 3.7.3 Question-set import

```text
User   QuestionListActivity  QuestionSetImportFlow  QuestionSetParser  QuestionSetImporter   DB
 │ Import      │                     │                    │                  │              │
 ├────────────▶├────────────────────▶│ launch OpenDocument│                  │              │
 │ pick file   │                     │                    │                  │              │
 ├─────────────────────────────────▶ │ read ≤ 1 MiB, UTF-8, background thread │              │
 │                                   ├───────────────────▶│ parse + validate all             │
 │                                   │◀───────────────────┤ QuestionSet or every violation   │
 │                                   ├──────────────────────────────────────▶│ plan()  (read)│
 │◀──────────────────────────────────┤ counts + Import / Cancel                              │
 │ confirm                           │                                                       │
 ├──────────────────────────────────▶├──────────────────────────────────────▶│ apply() ─────▶│ one transaction
 │◀──────────────────────────────────┤ result counts, list reloaded                          │
```

Nothing is written before the confirmation, and everything written is inside the single
transaction opened by `apply()` (REQ-IMP-20, REQ-IMP-40, REQ-IMP-80). DAO methods that open their
own transaction are safe to call inside it in one direction only: the platform treats a nested
`beginTransaction` as one transaction, so an inner frame can never commit on its own, and an
exception anywhere rolls the whole import back. The gap is the other direction — a write that
fails *without* raising, by returning `-1`, is not noticed by the caller and therefore does not
stop the outer transaction from committing. That is anomaly
[A-07](#6-known-deviations-and-design-debt).

### 3.8 State dynamics view

*Concern addressed:* the two states that outlive a single screen.

**Session.** `absent → signed in → absent`. `LoginActivity` reads the state on launch and skips
straight to `MainActivity` when it is present (REQ-AUTH-70); sign-out clears it and returns to the
sign-in screen (REQ-AUTH-80). The state lives in preferences, not in memory, so it survives
process death.

**Import.** A linear machine with one branch per failure, all of which end without a write:

```text
idle ─▶ picking ─▶ reading ─▶ validating ─▶ planning ─▶ confirming ─▶ applying ─▶ reporting ─▶ idle
          │           │            │                        │             │
          │           │            │                        │             └▶ failed (rollback)
          │           │            │                        └▶ cancelled (nothing written)
          │           │            └▶ invalid (violations listed, nothing written)
          │           └▶ unreadable / too large
          └▶ cancelled by the picker
```

**Activity lifecycle.** Every screen reloads its list in `onResume`, which is what makes an edit
made on another screen visible on return (REQ-USE-50) without a cross-activity notification
mechanism.

### 3.9 Algorithm view

*Concern addressed:* the four pieces of logic whose behaviour is not evident from their signature.

#### 3.9.1 Validation

`QuestionSetParser` collects violations instead of failing at the first one, and throws a single
`QuestionSetFormatException` carrying all of them (REQ-IMP-20). It rejects: an unsupported
`formatVersion`; a document that is not a single JSON object; any content after the closing brace;
unknown fields at either level; wrong types; empty or space-padded strings; tag names that span
lines; duplicate tags within one array; an absent or empty `questions` array; and any string
longer than the limits of [MF-IFS-001](question-import-format.md). A leading byte order mark is
stripped. The user interface shows at most ten violations followed by a count of the rest.

#### 3.9.2 Merge within a file

`MergedQuestion.mergeAll` keys entries by question text lower-cased, so repeated questions fold
into one. Tags are accumulated set-level first and then entry-level into a case-insensitive
ordered set, the first non-null answer wins, and the order of first appearance in the file is
preserved (REQ-IMP-70).

#### 3.9.3 Merge into the database

`QuestionSetImporter.apply` resolves each tag name through a case-insensitive cache, creating the
tag only when `TagDao.findByName` returns nothing (REQ-IMP-50). For each merged question it looks
up `findIdByName`: an existing question receives `addTags` and `fillMissingAnswer` — which cannot
overwrite an answer — and a new question is inserted (REQ-IMP-60).

#### 3.9.4 Credential derivation

`PasswordHasher` derives with `PBKDF2WithHmacSHA1`, 100 000 iterations, a 256-bit key and a
16-byte `SecureRandom` salt, encodes both as Base64 `NO_WRAP`, compares with
`MessageDigest.isEqual` so that comparison time does not depend on how many bytes match, and calls
`PBEKeySpec.clearPassword()` in a `finally` block (REQ-SEC-10, REQ-SEC-20). SHA-1 inside PBKDF2 is
chosen because `PBKDF2WithHmacSHA256` is only guaranteed from API level 26 while the product
baseline is 24 (decision D-16).

#### 3.9.5 Pattern construction

`SearchPatterns.like` trims the input and returns `"%"` when nothing is left, so an empty
criterion matches everything (REQ-SRCH-40); otherwise it returns the input unchanged, with no
escaping, which is what makes `%` and `_` user-facing wildcards (REQ-SRCH-60, decision D-08). The
value is always passed as a bound argument, never concatenated into SQL.

### 3.10 Resource view

*Concern addressed:* what the design consumes, and what bounds it.

| Resource | Design | Realises |
|---|---|---|
| Threads | The user-interface thread for screens and list queries; a single-thread executor in `LoginActivity` for credential derivation; a named `question-set-import` thread for reading and applying an import. No thread pool outlives the activity that created it. | REQ-PERF-40 |
| Database connections | One `MemForceDbHelper` instance for the process, obtained through a singleton accessor; the platform pools the underlying connections and applies `onConfigure` to each. | REQ-DB-50 |
| Memory — import | The chosen file is read whole into memory as UTF-8 text, bounded at 1 MiB (`MAX_FILE_BYTES`); anything larger is refused before it is read. | REQ-IMP-90, decision D-12 |
| Memory — lists | A query returns the matching rows as objects, with tag names already joined by SQL; at the reference volume this is a few thousand short strings. | REQ-PERF-60 |
| Storage | One database file, plus one small preferences file. No cache, no temporary file, no export. | REQ-EXT-60, REQ-DB-10 |
| Screen | Two themes, selected by the platform from `values/` and `values-night/`; no in-application switch and therefore no stored preference. | REQ-USE-60 |

### 3.11 Patterns use view

*Concern addressed:* the recurring structures a maintainer should recognise and continue.

| Pattern | Where | Why |
|---|---|---|
| **Contract class** | `DbContract` | Table and column names exist once; a rename is a compile error rather than a runtime one. |
| **Data access object** | `QuestionDao`, `TagDao`, `UserDao` | Confines SQL to one layer and gives every query a name that states its intent. Keeps rule 1 of [3.4](#34-dependency-view) checkable by inspection. |
| **Singleton helper** | `MemForceDbHelper` | One helper per process, so that `onConfigure` — and therefore foreign-key enforcement — applies to every connection the application uses. |
| **Plan / apply** | `QuestionSetImporter` | Separates "what would happen" from "make it happen", which is what allows the confirmation of REQ-IMP-40 to state accurate counts without a dry-run write. |
| **Generic view binding over `Named`** | `EntityAdapter`, `FilterSpinner` | Two list screens and one filter share one adapter and one spinner; a change to list behaviour is made once. |

---

## 4 Design rationale

### 4.1 Decisions inherited from the requirements

Decisions D-01 to D-19 in [MF-SRS-001, Annex B](requirements.md#annex-b--decision-record) are
requirement-level and are not restated here. This clause records only decisions that the
specification left open.

### 4.2 Decisions that shape the structure

| ID | Decision | Alternatives rejected |
|---|---|---|
| DD-01 | **Java, no Kotlin** (`android.builtInKotlin=false`). | Kotlin would suit the code well, but adds a compiler and a standard library to a product whose entire runtime dependency set is four libraries — AppCompat, ConstraintLayout, RecyclerView and Material Components. The decision is reversible per file if it is ever revisited. |
| DD-02 | **`SQLiteOpenHelper` and hand-written SQL, not Room.** | Room would generate the DAOs, but the schema is four tables and the two queries that matter — the `GROUP_CONCAT` join and the tag sub-select — are ones we would hand-write in Room anyway. Room also brings an annotation processor into a build that currently has none. |
| DD-03 | **Activities, no Fragments, no ViewModel.** | Each screen owns exactly one job, and the platform restores the text fields it manages. The cost is that state the platform does not manage — the tag selection in the question editor, and an import result whose screen was destroyed — is lost on recreation ([A-09](#6-known-deviations-and-design-debt)), and that the list screens re-query in `onResume` ([A-03](#6-known-deviations-and-design-debt)). A ViewModel layer is the answer if that state grows. |
| DD-04 | **Tag names joined in SQL with `GROUP_CONCAT`.** | Loading assignments per row would issue one query per list item; joining in the database keeps a search at the reference volume within REQ-PERF-10. |
| DD-05 | **Tag filter as a sub-select on `question_tags`, not a second join.** | A second join would multiply rows before `GROUP BY` and complicate the `GROUP_CONCAT`; the sub-select is served directly by `idx_question_tags_tag`. |
| DD-06 | **Import logic split between `importer` (pure Java) and `data` (database).** | Putting parsing in the DAO layer would make it untestable without an emulator; the split is what gives the product its 40 JVM unit tests (REQ-POR-40). |
| DD-07 | **Errors reported as return values (`-1`, `false`, `null`), not exceptions**, in the DAOs. | A duplicate tag name is an expected outcome of a user action, not an exceptional condition; the screens that call these methods handle both outcomes on the same path. `QuestionSetFormatException` is the deliberate exception: it carries a whole violation list, which no return value could. |
| DD-08 | **Seeded demo content on first creation.** | An empty first launch gives a user nothing to search, and makes the product look broken; 25 seeded questions demonstrate every screen. The demo *accounts* that come with it are the part that should not ship — [A-05](#6-known-deviations-and-design-debt). |

### 4.3 What the structure keeps open

The capabilities deferred in MF-SRS-001, 1.8 were checked against this design:

- **Personal collections** need one new table with an owning key and a filter in `QuestionDao`;
  no existing table changes.
- **Export** needs a serialiser over `QuestionSet`, which already models the file format
  independently of the database.
- **Media in questions** needs a column and a storage location; the import format reserves the
  field space for it ([MF-IFS-001](question-import-format.md), *Extensibility*).
- **Multi-tag filters** need the sub-select of DD-05 to take a set instead of one identifier.

---

## 5 Traceability

Every requirement of [MF-SRS-001](requirements.md) is realised by at least one design element.
The reverse direction is the "Realises" column throughout [clause 3](#3-design-views).

| Requirement group | Principal design elements |
|---|---|
| External interfaces (`EXT`) | [3.1](#31-context-view), [3.6.2](#362-external-interfaces), manifest, `SearchPatterns` |
| Authentication and session (`AUTH`) | `LoginActivity`, `UserDao`, `PasswordHasher`, `Session`, [3.7.1](#371-sign-in) |
| Questions (`QST`) | `QuestionListActivity`, `QuestionEditActivity`, `QuestionDao`, `TagPicker`, `EntityAdapter` |
| Tags (`TAG`) | `TagListActivity`, `TagEditActivity`, `TagDao`, `EntityAdapter` |
| Search and filtering (`SRCH`) | `SearchPatterns`, `QuestionDao.search`, `TagDao.search`, `FilterSpinner`, [3.7.2](#372-search) |
| Question-set import (`IMP`) | `QuestionSetImportFlow`, `QuestionSetParser`, `MergedQuestion`, `QuestionSetImporter`, [3.9](#39-algorithm-view) |
| Usability (`USE`) | `MainActivity`, confirmation dialogs, `strings.xml`, `themes.xml` + `values-night/themes.xml` |
| Performance (`PERF`) | [3.10](#310-resource-view), DD-04, DD-05, `idx_question_tags_tag` |
| Logical database (`DB`) | [3.5](#35-information-view), `MemForceDbHelper`, `DbContract` |
| Design constraints (`CON`) | [3.1](#31-context-view), [3.4](#34-dependency-view), manifest, `libs.versions.toml` |
| Standards compliance (`STD`) | [3.6.2](#362-external-interfaces), [MF-IFS-001](question-import-format.md), this document |
| Security (`SEC`) | [3.5.4](#354-credential-storage), [3.9.4](#394-credential-derivation), `Session`, manifest |
| Reliability (`REL`) | Transactions in `QuestionDao`, `UserDao`, `QuestionSetImporter`; [3.8](#38-state-dynamics-view) |
| Portability and maintainability (`POR`) | [3.4](#34-dependency-view), DD-06, `gradle/` toolchain, [MF-DEV-001](development-environment.md) |

---

## 6 Known deviations and design debt

Recorded here because a design description that hides them is worth less than one that does not.
Each is tracked as an anomaly in [MF-VVP-001](verification-and-validation.md), clause 9, where its
classification and disposition are held.

| ID | Deviation | Requirement affected | Consequence today | Intended resolution |
|---|---|---|---|---|
| A-01 | `MemForceDbHelper.onUpgrade` drops all four tables and recreates the schema. | REQ-DB-100 | None yet: the schema is at version 1, so `onUpgrade` has never run. The first schema change would destroy every installation's data. | Replace with versioned `ALTER TABLE` migrations **before** the schema version is raised. Blocking for any change to [3.5.1](#351-schema). |
| A-02 | The manifest sets `allowBackup="true"` and declares no backup rules, so the platform may copy `memforce.db` and the session preferences off the device. | REQ-SEC-60 | Content and credential material can leave the device through the platform backup transport, although the application itself opens no socket. | Set `allowBackup="false"`, or declare backup rules that exclude the database and `memforce_session`. |
| A-03 | List queries run on the user-interface thread (`onResume` and every keystroke). | REQ-PERF-10 at volume; REQ-PERF-40 is met, since it covers derivation, file reading and import writing. | Measured behaviour is within the limit at the seeded volume; at the reference volume of 2 000 questions it is a risk rather than a defect. | Move list queries to a background executor if measurement at the reference volume approaches 1 s. |
| A-04 | `questions.name` carries no uniqueness constraint, while import merges questions case-insensitively (REQ-IMP-60). | Consistency between REQ-QST-10 and REQ-IMP-60 | A user can type the same question twice; an import would have merged it. The two paths disagree about what "the same question" means. | Decide deliberately: either add a `UNIQUE COLLATE NOCASE` constraint and a merge on manual entry, or state in MF-SRS-001 that duplicates entered by hand are permitted. |
| A-05 | `DatabaseSeeder` creates the accounts `ana` and `marko` with a constant password in every build, including release. | REQ-SEC-10 in spirit; no requirement asks for demo accounts | Every fresh installation has two accounts whose password is in the source. Accounts are not a boundary between users' content (MF-SRS-001, 1.6), so the impact is limited to impersonation of a demo name. | Seed content without accounts, or gate account seeding to debug builds. |
| A-06 | No automated test executes any DAO, the schema, `PasswordHasher`, `Session` or any screen. | Verification coverage, not a product defect | 40 unit tests cover parsing and merging only; every database and user-interface requirement is verified by hand today. | Add instrumented tests for the DAOs and the schema, as planned in MF-VVP-001, clause 7.3. |
| A-07 | `QuestionDao.replaceTags` ignores the `-1` that `insert` returns on failure, and `insert`/`update` mark their transaction successful unconditionally. | REQ-IMP-80, REQ-REL-20 | A link write that fails **without raising** is not noticed: a question can be committed without some of its tags, inside an import that reports success. Rollback still works for anything that throws. | Use `insertOrThrow`, or check the return value and fail the transaction before it is marked successful. |
| A-08 | Failed writes are not reported: `QuestionEditActivity.save()` discards the identifier returned by `insert` and closes the editor either way; update and delete failures surface nothing to the user. | REQ-REL-30 | A question that was not stored looks stored until the list refreshes without it. | Return a typed outcome from the DAOs and report it at each call site without closing the form. |
| A-09 | State the platform does not restore is lost on activity recreation: the question editor's tag selection resets to the stored assignments on rotation, and an import result delivered to a destroyed screen is reported without its counts. | REQ-STD-10, REQ-IMP-100 | A rotation partway through editing silently discards tag changes the user made. | Persist the selection in `onSaveInstanceState`, and deliver the import result through lifecycle-aware state. |
| A-10 | The import counts questions the file *matches* rather than questions that actually gain a tag, while the result message reads "Updated with new tags". | Accuracy of the REQ-IMP-40 and REQ-IMP-100 counts | Re-importing an unchanged file reports questions as updated when nothing changed. | Either count only questions that gained an assignment, or reword the two messages to say "matched". |

---

## Annex A — Tailoring record

| # | Provision of IEEE Std 1016-2009 | Tailoring | Justification |
|---|---|---|---|
| T-1 | Clause 5 viewpoint catalogue | The structure viewpoint is not used; the remaining eleven are. | Stated with its reason in [Conformance and viewpoint selection](#conformance-and-viewpoint-selection). A viewpoint that would restate another adds volume, not information. |
| T-2 | Design overlays (clause 4.7) | Not used. | Overlays exist to combine views for a specialised audience; with eleven short views in one document, cross-references serve the same purpose. |
| T-3 | Formal design language (Annex B) | Prose, tables, ASCII diagrams and SQL are used instead of a modelling notation. | The design must stay correct in a repository where it is reviewed alongside code; a notation requiring a tool to render would drift. The ERD is the one exception and is kept as PlantUML source beside its rendered output. |
| T-4 | Separate identification of design entities with attribute tables | Elements are identified by their class name and package. | The names in [3.2](#32-composition-view) are the names in the source; a parallel identifier scheme would be a second thing to keep in step. |
