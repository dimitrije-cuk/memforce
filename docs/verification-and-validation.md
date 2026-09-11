# MemForce — Verification and Validation Plan

| Field | Value |
|---|---|
| Document title | MemForce — Verification and Validation Plan |
| Document identifier | MF-VVP-001 |
| Version | 1.0 |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Subject | MemForce, version 1.0 |
| Information item | Verification and Validation Plan (VVP) |
| Conforms to | IEEE Std 1012-2016, clause 12; test documentation per ISO/IEC/IEEE 29119-1:2013 concepts; reviews per IEEE Std 1028-2008; anomaly classification per IEEE Std 1044-2009 |
| Verifies | [MF-SRS-001](requirements.md), [MF-SDD-001](design.md) |

---

## 1 Purpose and scope

This plan states how MemForce is **verified** — shown to satisfy [MF-SRS-001](requirements.md)
and to have been built as [MF-SDD-001](design.md) describes — and how it is **validated** — shown
to meet the stakeholder needs of MF-SRS-001, Annex A.1.

Its scope is the whole product: the application package, the database schema, the question-set
file interface, and the documentation set. It covers the development, transition and maintenance
stages of the product's life cycle.

The acceptance criteria for each individual requirement are **not repeated here**; they live in
[MF-SRS-001, clause 4](requirements.md#4-verification), next to the requirement they judge. This
plan states who performs them, in what environment, in what order, when they are considered
passed, and what happens when they fail.

---

## 2 Referenced documents

| Reference | Role |
|---|---|
| [MF-SRS-001](requirements.md) — Software Requirements Specification | The specification being verified; holds the per-requirement acceptance criteria. |
| [MF-SDD-001](design.md) — Software Design Description | The design being verified, and the source of the known deviations in [clause 9](#9-anomaly-management). |
| [MF-CMP-001](configuration-management.md) — Configuration Management Plan | Defines the baselines that V&V results are recorded against. |
| [MF-IFS-001](question-import-format.md) — Question import format | The interface specification exercised by the import test suite. |
| [MF-DEV-001](development-environment.md) — Development environment | Defines the toolchain in which the automated tests execute. |
| IEEE Std 1012-2016 | V&V processes, integrity levels, the VVP outline of clause 12, and the method vocabulary. |
| IEEE Std 1028-2008 | Review and audit types, and their entry and exit criteria. |
| IEEE Std 1044-2009 | Anomaly classification, severity and disposition. |
| ISO/IEC/IEEE 29119-1:2013 | Test concepts: test levels, sub-processes, risk-based testing, test design techniques. |
| ISO/IEC/IEEE 15939:2017, IEEE Std 982.1-2005 | The measurement information model and measure definitions used in [clause 11](#11-measures). |

---

## 3 Definitions

| Term | Meaning as used here |
|---|---|
| **Verification** | Confirmation that a work product meets its specification. |
| **Validation** | Confirmation that the product meets the stakeholder needs in actual use. |
| **Integrity level** | The IEEE 1012 measure of the consequence of failure, which selects how much V&V is done. |
| **Test level** | A group of test activities organised together: unit, integration, system, acceptance. |
| **Reference device / reference volume** | The measurement conditions defined in MF-SRS-001, 3.4 and assumption A-4. |
| **Anomaly** | Any condition that deviates from expectation, per IEEE 1044-2009 — a defect in the product, in a document, or in a test. |
| **Baseline** | A configuration of the product and its documents, labelled and controlled under MF-CMP-001. |

Terms not defined here carry the meaning given in ISO/IEC/IEEE 24765:2017.

---

## 4 V&V overview

### 4.1 Organisation and independence

MemForce is developed by a very small team. V&V is performed by the development team, with the
following separation of concerns rather than of organisations:

| Role | Responsibility |
|---|---|
| Author | Produces the work product; runs the automated suites before proposing a change. |
| Reviewer | Reviews the change against this plan's criteria; must not be the author of the change under review. |
| V&V lead | Maintains this plan, the anomaly register, and the record of which requirements have been verified against which baseline. |

**Independence.** No independent V&V organisation exists (IEEE 1012-2016 defines several forms of
independence; none is claimed). This is a deliberate tailoring, recorded in
[clause 13](#13-tailoring-record), and is acceptable at the assigned integrity level. The
compensating control is that the reviewer of a change is never its author, and that every
requirement carries a stated, objective acceptance criterion so that verification does not depend
on the verifier's judgement of "good enough".

### 4.2 Integrity level assignment

IEEE Std 1012-2016 scales V&V effort by integrity level, derived from the consequence of a
failure.

| Consequence considered | Assessment for MemForce |
|---|---|
| Injury or death | None. The product has no actuation, no medical, vehicular or safety function. |
| Financial loss | None. No payment, no licence, no business record. |
| Regulatory or legal exposure | None in version 1.0: no personal data is collected beyond a user-chosen name, and nothing is transmitted (REQ-EXT-50). |
| Loss of user data | **The material consequence.** A failure can destroy questions a learner has entered over months, recoverable only by re-entering them. |
| Loss of service | A learner cannot study from the application until it is reinstalled. |

**Assigned integrity level: 2** (minor consequence — the affected function's failure degrades
the user's ability to use the product and can lose their data, with no safety, financial or
regulatory effect).

The assignment selects the V&V tasks in [clause 5](#5-vv-tasks-by-life-cycle-activity). The
data-loss consequence is why the tasks that guard persistence — schema migration, transaction
atomicity, and import rollback — are treated as mandatory at every baseline, while tasks such as
hazard analysis and independent audit are not performed at all.

### 4.3 Master schedule

V&V is continuous rather than staged: every change passes the same gates before it enters the
baseline.

| When | V&V activity | Output |
|---|---|---|
| Per change (before merge) | Automated suite; review by a non-author; requirement-impact check | Review record in the change history |
| Per change touching the schema, `data`, `importer` or `security` | The affected suites of [clause 8](#8-test-suites-and-procedures) re-executed | Updated verification record |
| Per release candidate | Full manual system suite; performance measurement at the reference volume; installation check at both platform levels | Verification record for the baseline |
| Per release | Validation against the product functions of MF-SRS-001, 1.4; configuration audit per MF-CMP-001, clause 7 | Release record |
| On any anomaly report | Classification, disposition, and regression coverage for the fix | Anomaly register entry |

### 4.4 Resources and environment

| Resource | Definition |
|---|---|
| Build environment | The toolchain of [MF-DEV-001](development-environment.md): Gradle wrapper 9.4.1, Android Gradle Plugin 9.2.1, JDK 17 or newer, `compileSdk` 34. |
| JVM test environment | `gradlew testDebugUnitTest`; JUnit 4.13.2 with `org.json` on the test classpath. No emulator required. |
| Device environment — minimum | Emulator or device at API level 24 (the declared minimum, MF-SRS-001 A-1). |
| Device environment — target | Emulator or device at API level 34. |
| Reference device | At least 2 GB RAM and four cores at 1.4 GHz, running API level 24 (MF-SRS-001 A-4). |
| Reference volume | 2 000 questions, 300 tags, 8 000 assignments, 5 accounts, generated by importing question-set files built for the purpose (MF-SRS-001, 3.4). |
| Seed data | The 25 questions, 9 tags and 53 assignments created on first launch (MF-SDD-001, 3.5.5) — the starting state of every manual procedure unless the procedure states otherwise. |

### 4.5 Tools, techniques and methods

| Purpose | Means |
|---|---|
| Automated unit testing | JUnit 4 on the JVM, run by Gradle. |
| Static checking | The Android Gradle Plugin's compilation and lint stages, and review by a person. |
| Database inspection | `adb shell` with the SQLite command-line tool against the application's private database in a debug build. |
| Measurement of response times | Wall-clock measurement from the user action to the displayed result, ten repetitions per case, worst case recorded. |
| Method vocabulary (`I`, `A`, `D`, `T`) | IEEE Std 1012-2016, as defined in [MF-SRS-001, 4.1](requirements.md#41-verification-methods). |

---

## 5 V&V tasks by life-cycle activity

The tasks below are those selected for integrity level 2. Each states what makes it pass.

| Activity | V&V task | Pass condition |
|---|---|---|
| Requirements | Requirement evaluation: each requirement is checked against the nine characteristics of ISO/IEC/IEEE 29148:2018, clause 5.2.5. | The self-audit in [MF-SRS-001, Annex D](requirements.md#annex-d--requirement-quality-self-audit) holds for the current version. |
| Requirements | Traceability analysis: every requirement traces back to a stakeholder need or a recorded decision, and forward to a verification entry. | The check of [clause 6](#6-verification-coverage) reports no gap. |
| Design | Design evaluation: every requirement has at least one realising design element, and every design element serves a requirement. | [MF-SDD-001, clause 5](design.md#5-traceability) is complete in both directions. |
| Design | Interface analysis: the four external interfaces of MF-SDD-001, 3.1 are each specified and each verified. | Suites TS-EXT and TS-IMP pass. |
| Implementation | Source evaluation: the layering rules of MF-SDD-001, 3.4 hold; no SQL outside `data` and `db`; no Android type in `importer`. | Review by inspection at each change; TS-POR passes. |
| Implementation | Automated unit testing of the parsing and merging logic. | `gradlew testDebugUnitTest` passes with no skipped test. |
| Test | Test evaluation: every requirement is covered by at least one procedure of [clause 8](#8-test-suites-and-procedures). | The coverage table of [clause 6](#6-verification-coverage) shows no uncovered requirement. |
| Integration | Database integration testing: schema constraints, cascades and transactions behave as MF-SRS-001, 3.5 requires. | Suite TS-DB passes. *Currently manual — see [A-06](#92-open-anomalies).* |
| System | System testing against the functional and quality requirements. | Suites TS-AUTH … TS-POR pass on both platform levels. |
| Validation | Operation of the product against the product functions of MF-SRS-001, 1.4 by a person acting as a learner. | Every function group is exercised end to end without recourse to this documentation. |
| Maintenance | Regression: the suites covering the changed area, plus TS-DB whenever the schema or a DAO changes. | Same pass conditions as above, against the new baseline. |

Tasks defined by IEEE 1012-2016 that are **not** performed: hazard analysis, security
vulnerability analysis by an independent party, risk analysis reports as separate items,
acquisition and supply support tasks, and independent audits. See
[clause 13](#13-tailoring-record).

---

## 6 Verification coverage

MF-SRS-001 states 118 requirements, each with exactly one verification entry. Distribution by
method:

| Method | Requirements | Where executed |
|---|---|---|
| Test (`T`) | 82 | JVM unit tests and the device suites of [clause 8](#8-test-suites-and-procedures) |
| Inspection (`I`) | 21 | Source, manifest, schema and document review |
| Demonstration (`D`) | 12 | Manual operation on a device or emulator |
| Analysis (`A`) | 3 | Reasoning over design and measured data |
| **Total** | **118** | |

Coverage is checked mechanically at each baseline: every `REQ-` identifier defined in
MF-SRS-001, clause 3 must appear exactly once in clause 4, and every identifier must appear in at
least one suite of [clause 8](#8-test-suites-and-procedures). A failure of this check blocks the
baseline.

---

## 7 Test approach

### 7.1 Test levels

Following the test sub-process model of ISO/IEC/IEEE 29119-1:2013:

| Level | Scope | Current state |
|---|---|---|
| **Unit** | Parsing, validation and merging (`com.memforce.importer`); the search criteria and the statements built from them (`com.memforce.search`, `com.memforce.data`); answer marking and the rules of a game (`com.memforce.game`). Runs on the JVM, no device. | Automated: 7 test classes, 103 tests. |
| **Integration (database)** | DAOs against a real SQLite database: constraints, cascades, transactions, collation, pattern search. | **Not automated** — anomaly [A-06](#92-open-anomalies). Executed manually via TS-DB. |
| **System** | The application on a device or emulator, against MF-SRS-001 clause 3 as a whole. | Manual, by the suites of [clause 8](#8-test-suites-and-procedures). |
| **Acceptance / validation** | A person uses the product as a learner against MF-SRS-001, 1.4. | Manual, per release. |

### 7.2 Test design techniques

Risk-based selection per ISO/IEC/IEEE 29119-1:2013, clause 5. The highest-risk areas are those
that write many records at once (import), those that guard data (schema, transactions) and those
that cannot be undone (deletion, migration); they receive the most cases. Deletion is now also
reachable by a gesture, which adds the risk of an unintended one, so every swipe case checks both
outcomes of its confirmation.

| Technique | Applied to |
|---|---|
| Equivalence partitioning | Input fields: empty, valid, over-length; pattern criteria: empty, literal, wildcard; submitted answers: exact, differing only in case or spacing, differing in a word, empty. |
| Boundary value analysis | Field limits of MF-IFS-001 (120, 500, 50, 1 000, 2 000 characters); the 1 MiB file bound; the 10-violation display cap; a game of one question; a lobby of none; the last question of a game. |
| Decision table | Sign-in: {name known, unknown} × {password matches, does not} × {fields empty}. Import merge: {question exists, does not} × {stored answer present, absent}. Game end: {every question answered correctly, not} × {streak equals the total, does not}. |
| State transition | The import state machine of MF-SDD-001, 3.8, including every failure exit. The queue of a game: the question asked moves to the back on each of a correct, an incorrect and an empty submission. |
| Error guessing / negative testing | Malformed JSON, trailing content after the closing brace, byte order marks, non-breaking spaces in tags, unknown fields, quotation and pattern characters in every text field; a question deleted while it is in the lobby; a tag carrying no question swiped towards the lobby. |
| Data-volume testing | All performance procedures, executed only at the reference volume. |

### 7.3 Automated test inventory

Executed by `gradlew testDebugUnitTest`.

| Test class | Tests | Covers |
|---|---|---|
| `com.memforce.importer.QuestionSetParserTest` | 34 | Format acceptance and the rejection rules it exercises: version support, missing required fields, unknown fields at both levels, non-object roots, blank and space-padded values, the question and tag length limits, byte order mark, trailing content, single-line tag names, duplicate tags within an array, an empty `questions` array, and the collection of all violations into one failure. Parses the published template and example. Requirements: REQ-IMP-20, REQ-IMP-30, REQ-STD-30, REQ-POR-40. |
| `com.memforce.importer.MergedQuestionTest` | 6 | Set-level tags precede question-level tags; duplicate tags fold; questions without tags; file order preserved; a repeated question folds into one; the first answer wins. Requirements: REQ-IMP-70. |
| `com.memforce.search.SearchQueryTest` | 12 | The criteria a search carries: empty means no criterion; text of spaces is no criterion; tags keep the order they were chosen in and are held once; dropping and toggling a tag; text changes leave the tags alone; the value is immutable, so the criteria a screen has already used cannot be altered behind it. Requirements: REQ-SRCH-40, REQ-SRCH-100. |
| `com.memforce.data.QuestionFilterTest` | 11 | The condition that decides which questions match: the text pattern is asked of the question text and of the tag names, so title and tag matches land in one result; each chosen tag adds a further condition, never an alternative one; the patterns are trimmed and an empty pattern matches everything; each tag condition is named apart from the others. Requirements: REQ-SRCH-30, REQ-SRCH-40, REQ-SRCH-60, REQ-SRCH-90, REQ-SRCH-100. |
| `com.memforce.data.TagQueriesTest` | 11 | The tag statements: the tag list counts by a sub-select, so a tag no question carries is still read; the three orders and their tie-break by name; the suggestions are drawn only from the questions the search already found, are counted within them, exclude the tags already chosen, and are limited. Requirements: REQ-TAG-70, REQ-TAG-90, REQ-SRCH-110, REQ-SRCH-120. |
| `com.memforce.game.AnswerMatcherTest` | 11 | What marking forgives — letter case, surrounding spacing, the length of spacing runs — and what it does not: a missing word, a different answer, an empty submission, and a question with no stored answer. Requirements: REQ-GAME-80. |
| `com.memforce.game.GameSessionTest` | 18 | The rules of a run: a game needs a question; the shuffle loses nothing and repeats for a fixed source; every other question is asked before one is asked again, whatever the outcome; the streak breaks on a wrong answer and on a skip; a question answered correctly twice counts once; victory when every question has been answered correctly; perfect victory when the streak covers the whole set; no submission is taken once the run is decided. Requirements: REQ-GAME-60, REQ-GAME-70, REQ-GAME-80, REQ-GAME-90, REQ-GAME-100, REQ-GAME-110, REQ-GAME-120. |
| **Total** | **103** | |

Last executed on 2026-09-11 against the current draft: 103 tests, 0 failures, 0 errors, 0 skipped.

Rules of MF-IFS-001 that the automated suite does **not** yet exercise, and which TS-IMP therefore
covers by hand: the `name` and `description` length limits, a wrongly typed `answer`, `tags` or
`questions` value, and the 1 MiB file bound and unreadable-stream case of REQ-IMP-90 — the last
two belong to the import flow rather than to the parser and cannot be reached without a document
provider.

**Coverage gaps, and what compensates for them.** No automated test executes a DAO, the schema,
`PasswordHasher`, `Session`, `Lobby` or any screen (anomaly [A-06](#92-open-anomalies)). The
statements the DAOs run are built by `QuestionFilter` and `TagQueries` and are covered by the JVM
suite, but nothing automated runs them against SQLite, so what the statements *return* — and with
it the narrowing of [REQ-SRCH-120](requirements.md#324-search-and-filtering) and the counts of
[REQ-TAG-70](requirements.md#323-tags) — is still established by hand. Until instrumented tests
exist, those requirements are verified by the manual suites of
[clause 8](#8-test-suites-and-procedures) at every release candidate, and any change to `db`,
`data` or `security` requires the corresponding suite to be re-executed before merge
([4.3](#43-master-schedule)).

Planned closure, in priority order: TS-DB (schema, constraints, cascades, transactions) together
with the search and suggestion statements, which need the same database fixture; then
`PasswordHasher` and `SearchPatterns` — both of which need no emulator and could move to the JVM
suite with a thin abstraction — then the screens.

### 7.4 Entry, exit, suspension and resumption

| Criterion | Definition |
|---|---|
| **Entry** | The build compiles; the automated suite passes; the change is under review; the requirements affected by the change are identified. |
| **Exit — per change** | Every suite covering the changed area passes; no new anomaly of severity *major* or above is open against the change. |
| **Exit — per release** | Every requirement of MF-SRS-001 has a passing verification record against the same build; every open anomaly is either *minor* or has a recorded, accepted disposition; the configuration audit of MF-CMP-001, clause 7 passes. |
| **Suspension** | Testing stops when the build does not install, when the database cannot be created, or when a data-loss anomaly is observed; the cause is fixed before testing resumes. |
| **Resumption** | The suspended suite restarts from its beginning against a fresh installation, not from the point of suspension. |

---

## 8 Test suites and procedures

Each suite states the requirements it covers and how it is executed. The pass criteria for each
requirement are those in [MF-SRS-001, clause 4](requirements.md#4-verification); a suite passes
when every criterion it covers is met.

| Suite | Requirements covered | Level | Execution |
|---|---|---|---|
| **TS-EXT** — interfaces and navigation | REQ-EXT-10 … REQ-EXT-70 | System | Manual, both platform levels |
| **TS-AUTH** — sign-in, registration, session | REQ-AUTH-10 … REQ-AUTH-80 | System | Manual; database state inspected with the SQLite tool |
| **TS-QST** — question management | REQ-QST-10 … REQ-QST-90 | System | Manual |
| **TS-TAG** — tag management | REQ-TAG-10 … REQ-TAG-110 | System | Manual |
| **TS-SRCH** — search and filtering | REQ-SRCH-10 … REQ-SRCH-150 | Unit + system | JUnit for the criteria and the statements built from them; manual on both screens, against the prepared pattern data set of [8.1](#81-prepared-data-sets) |
| **TS-IMP** — question-set import | REQ-IMP-10 … REQ-IMP-100, REQ-STD-30 | Unit + system | JUnit for parsing and merging; manual for picking, confirming, applying and rollback |
| **TS-GAME** — lobby and game | REQ-GAME-10 … REQ-GAME-120 | Unit + system | JUnit for marking and the rules of a run; manual for the three ways into the lobby, its retention, and the two endings |
| **TS-USE** — usability | REQ-USE-10 … REQ-USE-80 | System | Manual, in light and in dark appearance |
| **TS-PERF** — performance | REQ-PERF-10 … REQ-PERF-60 | System | Manual measurement at the reference volume, procedure [8.2](#82-performance-measurement) |
| **TS-DB** — schema and integrity | REQ-DB-10 … REQ-DB-100 | Integration | Manual through the SQLite tool, procedure [8.3](#83-database-integrity) |
| **TS-CON** — constraints | REQ-CON-10 … REQ-CON-50 | System | Inspection of the manifest, the dependency set and the package; flight-mode run |
| **TS-STD** — standards compliance | REQ-STD-10, REQ-STD-20, REQ-STD-30 | Analysis / inspection | Document check against the clause map; schema validation of the published files |
| **TS-SEC** — security attributes | REQ-SEC-10 … REQ-SEC-60 | System + inspection | Source and manifest inspection; database inspection after sign-in |
| **TS-REL** — reliability | REQ-REL-10 … REQ-REL-40 | System | Manual, with induced failures per [8.4](#84-induced-failure) |
| **TS-POR** — portability and build | REQ-POR-10 … REQ-POR-40 | System | Clean-checkout build; installation at API 24 and 34; import inspection |

### 8.1 Prepared data sets

| Data set | Content | Used by |
|---|---|---|
| `DS-PATTERN` | Questions and tags chosen so that each of `po%`, `%ta`, `%sto%`, `_br%`, `%__a` has a known, non-empty expected result and a known non-matching neighbour. | TS-SRCH (REQ-SRCH-60) |
| `DS-OVERLAP` | Tags of known and unequal use, including one tag carried by no question, two tags carried by the same questions, and two tags sharing no question; and a question carried by twelve tags. | TS-SRCH (REQ-SRCH-110, REQ-SRCH-120), TS-TAG (REQ-TAG-70, REQ-TAG-90), TS-QST (REQ-QST-80) |
| `DS-UNANSWERED` | A set of five questions of which two carry no answer. | TS-GAME (REQ-GAME-50) |
| `DS-CASE` | A tag `Algebra` and a question differing from a stored one only by letter case. | TS-AUTH, TS-TAG, TS-IMP (REQ-AUTH-60, REQ-TAG-20, REQ-IMP-60) |
| `DS-VOLUME` | Question-set files that together produce the reference volume. | TS-PERF |
| `DS-BAD` | One file per rejection rule of MF-IFS-001, plus one file carrying several violations at once, one of 2 MiB, and one holding invalid JSON. | TS-IMP (REQ-IMP-20, REQ-IMP-90) |

### 8.2 Performance measurement

1. Install a clean build; import `DS-VOLUME` until the reference volume is reached.
2. For each of text search, tag filter and combined filter: perform ten searches with different
   criteria, measuring from the last keystroke to the displayed list. Record the worst case. The
   measurement includes the tags offered beneath the field, because they are read on the same
   change as the results ([REQ-SRCH-110](requirements.md#324-search-and-filtering)).
3. Create, edit and delete ten questions and ten tags, measuring from the confirming action to
   the updated list. Record the worst case.
4. Cold-start the application five times, measuring to the first accepted input. Record the mean.
5. Import a 200-question set, measuring from confirmation to the result dialog.
6. Record the device, its API level and the volume with the results. A measurement without its
   conditions is not a result.

### 8.3 Database integrity

Executed against a debug build with the SQLite command-line tool.

1. Confirm the four tables, their constraints and the index exist as MF-SDD-001, 3.5.1 states.
2. Insert an assignment naming a non-existent question; confirm the database rejects it
   (REQ-DB-50).
3. Delete a tag carried by three questions; confirm exactly three assignment rows disappear and
   all three questions remain (REQ-DB-60).
4. Delete a question carrying two tags; confirm exactly two assignment rows disappear and both
   tags remain (REQ-DB-70).
5. Insert a user name and a tag name differing from existing ones only by case; confirm the
   database — not the user interface — rejects both (REQ-DB-80).
6. Delete the highest-keyed question, insert a new one, and confirm the key is not reused
   (REQ-DB-30).
7. Close the application, restart the device, and confirm every record is still present
   (REQ-DB-90).
8. **Migration:** take a database file created by the previous schema version, install the current
   build over it, and confirm every question, tag, assignment and account is readable afterwards
   (REQ-DB-100). *This procedure currently fails by construction — see
   [A-01](#92-open-anomalies).*

### 8.4 Induced failure

1. **Import rollback:** make the database read-only after the confirmation but before the
   transaction commits, or use a debug build that throws inside `apply()`. Confirm the tables are
   exactly as before (REQ-IMP-80, REQ-REL-20).
2. **Delete atomicity:** deleting a question is a single `DELETE` whose cascade runs inside that
   statement, so there is no application-visible point at which to interrupt it. Verify instead by
   inspection that both foreign keys declare `ON DELETE CASCADE` and by [8.3](#83-database-integrity)
   steps 3 and 4 that the cascade takes effect. The multi-statement paths — question create,
   question update and import — are the ones exercised by step 1 and by the induced failure of
   REQ-REL-20.
3. **Process death:** kill the process while idle; confirm the next launch shows the data of the
   last completed operation, and that the session survives (REQ-REL-40, REQ-AUTH-70).
4. **Write failure:** with the database unwritable, attempt a create; confirm the failure is
   reported and the application remains operable (REQ-REL-30).

---

## 9 Anomaly management

### 9.1 Classification and workflow

Anomalies are classified per IEEE Std 1044-2009: each is recorded with what was observed, where
it was found, its severity, and its disposition.

| Severity | Meaning here |
|---|---|
| **Critical** | Data is lost or corrupted, or the product cannot be used at all. |
| **Major** | A specified requirement is not met and no reasonable workaround exists. |
| **Minor** | A requirement is met imperfectly, or a workaround exists. |
| **Informational** | No requirement is violated; the item is debt, a documentation gap, or a risk. |

Workflow: **recognised → investigated → classified → disposed**. Disposition is one of *fixed*,
*deferred with a stated trigger*, *accepted with a recorded justification*, or *rejected as not an
anomaly*. Every fix requires a test that would have caught the anomaly, added to the suite that
covers the affected requirement; where the level is not yet automated, the manual procedure is
extended instead.

### 9.2 Open anomalies

Recorded against the current draft and carried into baseline BL-1 when it is declared. These are
the same items as
[MF-SDD-001, clause 6](design.md#6-known-deviations-and-design-debt); this is where their
classification and disposition live.

| ID | Anomaly | Requirement | Severity | Disposition |
|---|---|---|---|---|
| **A-01** | `onUpgrade` drops and recreates all tables, so raising the schema version would destroy every installation's data. | REQ-DB-100 | Major (latent — the schema is at version 1, so the code path has never executed) | **Deferred with trigger.** Blocking: the schema version may not be raised until versioned migrations replace it. Procedure [8.3](#83-database-integrity) step 8 stays failing until then. |
| **A-02** | `allowBackup="true"` with no backup rules; the platform may copy the database and the session record off the device. | REQ-SEC-60 | Major | **Open — to fix.** Set `allowBackup="false"` or exclude the database and `memforce_session` by rule; verify by inspection under TS-SEC. |
| **A-03** | List queries execute on the user-interface thread. | REQ-PERF-10 at the reference volume | Minor (risk) | **Accepted for BL-1**, conditional on measurement: if procedure [8.2](#82-performance-measurement) records worse than 0.8 s, move list queries to a background executor. |
| **A-04** | Question text has no uniqueness constraint, while import merges case-insensitively; manual entry and import disagree about what "the same question" is. | Consistency of REQ-QST-10 with REQ-IMP-60 | Minor | **Open — decision required.** Either add a case-insensitive uniqueness constraint with a merge on manual entry, or state in MF-SRS-001 that hand-entered duplicates are permitted. |
| **A-05** | Demo accounts `ana` and `marko` are created with a constant password in every build, including release. | No requirement asks for them; contrary to the intent of REQ-SEC-10 | Minor | **Open — to fix.** Seed content without accounts, or gate account seeding to debug builds. |
| **A-06** | No automated test executes a DAO, the schema, `PasswordHasher`, `Session`, `Lobby` or any screen. The statements the DAOs run are covered on the JVM, but nothing automated runs them against SQLite. | Verification coverage | Major (process) | **Open — planned.** Add database integration tests first ([7.3](#73-automated-test-inventory)); until then the manual suites are mandatory per change, as [4.3](#43-master-schedule) requires. |
| **A-07** | `QuestionDao.replaceTags` ignores a failed `insert` (`-1`), and `insert`/`update` mark their transaction successful unconditionally, so a link write that fails without raising does not stop the enclosing import from committing. | REQ-IMP-80, REQ-REL-20 | Major | **Open — to fix.** Use `insertOrThrow` or check the return value before marking the transaction successful. Regression cover: a DAO test that forces a link insert to fail, added with the TS-DB automation of A-06. |
| **A-08** | Failed writes are not reported to the user: the question editor discards the result of `insert` and closes either way; update and delete failures are not surfaced. | REQ-REL-30 | Major | **Open — to fix.** Return a typed outcome from the DAOs and report it at the call site without closing the form. |
| **A-09** | State the platform does not restore is lost on recreation: the question editor's tag selection resets on rotation; an import result delivered to a destroyed screen omits its counts. | REQ-STD-10, REQ-IMP-100 | Minor | **Open — to fix.** Save the selection in `onSaveInstanceState`; deliver the import result through lifecycle-aware state. |
| **A-10** | Import counts questions the file matches rather than questions that gain a tag, while the message reads "Updated with new tags". | Accuracy of REQ-IMP-40 and REQ-IMP-100 counts | Minor | **Open — decision required.** Count only questions that gained an assignment, or reword both messages to "matched". |

No anomaly of severity *critical* is open.

---

## 10 Reviews and audits

Review types follow IEEE Std 1028-2008. Two are used.

| Review | When | Entry | Exit |
|---|---|---|---|
| **Technical review** | Every change to source or documents, before it enters the baseline. | The change is complete, the automated suite passes, and the requirements it affects are named. | The reviewer — never the author — confirms: the change meets the requirements it claims; documentation affected by it has been updated in the same change; no new anomaly of severity *major* or above is introduced. |
| **Inspection** | On the schema, on `security`, and on any change to the import format. | The item and its specification are both available. | Every rule in the specification is checked line by line against the item, and each finding is recorded as an anomaly. |

**Management reviews, walk-throughs and independent audits** are not performed; see
[clause 13](#13-tailoring-record). The configuration audit that confirms a release matches its
baseline is defined in [MF-CMP-001, clause 7](configuration-management.md).

---

## 11 Measures

A small measurement set, defined per the information model of ISO/IEC/IEEE 15939:2017 (information
need → measure → decision criterion). Measures that would cost more to collect than they inform
are not defined.

| Information need | Measure | Collected | Decision criterion |
|---|---|---|---|
| Are the requirements verified? | Requirement verification coverage: requirements with a passing record ÷ 118 | Per release candidate | Must be 100 % before a release baseline. |
| Is verification automated enough for the change rate? | Automation ratio: requirements verified by an automated test ÷ requirements verified by method `T` | Per release | A fall below the previous release is investigated; the trend, not the value, is what matters while A-06 is open. |
| Is the product stable? | Open anomalies by severity | Continuous | No *critical*; no *major* without a recorded disposition. |
| Does the product still meet its performance limits? | Worst-case search, single-operation and cold-start times at the reference volume | Per release candidate | Within MF-SRS-001, 3.4; a value within 20 % of a limit is recorded as a risk. |
| Is the specification stable? | Requirements added, changed or withdrawn per baseline | Per baseline | Sustained growth without a corresponding change in verification coverage indicates that clause 4 of MF-SRS-001 is falling behind clause 3. |

---

## 12 Reporting and records

| Record | Content | Where kept |
|---|---|---|
| Verification record | Per baseline: each requirement, the method, the date, the build, the result | With the baseline under [MF-CMP-001](configuration-management.md) |
| Automated test result | Gradle test report | Build output, regenerated on demand from the tagged commit |
| Anomaly register | [Clause 9.2](#92-open-anomalies) of this document | This document, under change control |
| Measurement record | The values of [clause 11](#11-measures) with their conditions | With the baseline |
| Review record | What was reviewed, by whom, findings and their disposition | The change history of the repository |

A result without its build identifier and its conditions is not a record: it cannot be reproduced
and it cannot be trusted after the next change.

---

## 13 Tailoring record

| # | Provision | Tailoring | Justification | Risk accepted |
|---|---|---|---|---|
| T-1 | Independent V&V (IEEE 1012-2016, Annex C) | No independence of organisation, budget or management is claimed. | No separate organisation exists. At integrity level 2, IEEE 1012 does not require independence. | Author bias in verification. Compensated by objective per-requirement criteria and by the rule that a reviewer is never the author. |
| T-2 | Hazard, security and risk analysis tasks | Not performed as separate analyses. | No safety function; no network interface; no personal data beyond a chosen name. The security properties that do matter are stated as requirements (MF-SRS-001, 3.8.1) and verified by TS-SEC. | A security weakness outside the stated requirements would not be found by a systematic method. Anomaly A-02 is an example found by inspection rather than by analysis. |
| T-3 | Separate test plan, test design, test case and test procedure documents (ISO/IEC/IEEE 29119-3) | All test documentation is held in this plan and in MF-SRS-001, clause 4. | Separating them would duplicate every acceptance criterion, and the duplicate would drift. | Test documentation is less granular than the standard's model; suites, not individual cases, are the unit of record. |
| T-4 | Acquisition, supply and installation V&V activities | Not performed. | There is no acquirer, no supplier and no installation beyond the platform's own package installer. | None material. |
| T-5 | Management reviews and walk-throughs (IEEE 1028-2008, clauses 4 and 7) | Not performed; technical review and inspection are. | With one team and a continuous flow of changes, progress is visible in the repository; a walk-through would repeat the technical review. | Alternatives to a design are explored less systematically than a walk-through would force. |
