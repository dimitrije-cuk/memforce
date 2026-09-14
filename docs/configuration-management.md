# MemForce — Configuration Management Plan

| Field | Value |
|---|---|
| Document title | MemForce — Configuration Management Plan |
| Document identifier | MF-CMP-001 |
| Version | 1.1 |
| Date | 2026-09-14 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Subject | The MemForce repository and everything it delivers |
| Information item | Configuration Management Plan (CMP) |
| Conforms to | IEEE Std 828-2012, normative Annex D (CMP content) |

---

## 1 Purpose and scope

This plan states how the parts of MemForce are identified, how they are changed, how their state
is recorded, and how a delivered release is shown to match what was approved.

It covers every item in the repository: source, build definition, resources, seed data, the
question-set interface files, and the documentation set. It applies from the first commit to the
retirement of the product, and it binds every change, including changes made by a single person
working alone.

**Why a plan at this size.** Configuration management is what makes a result reproducible. Without
it, "it worked" is a statement about a machine on a day rather than about a product; a defect
cannot be tied to a version, and a document cannot be trusted to describe the code beside it. The
process below is deliberately small enough to be followed on every change, because a process that
is skipped under pressure controls nothing.

---

## 2 Referenced documents

| Reference | Role |
|---|---|
| IEEE Std 828-2012 | The CM process and the CMP content of Annex D. |
| [MF-SRS-001](requirements.md) | Requirements — a controlled item; the source of the change impact analysis. |
| [MF-SDD-001](design.md) | Design — a controlled item. |
| [MF-VVP-001](verification-and-validation.md) | The verification gates a change must pass, and the anomaly register. |
| [MF-IFS-001](question-import-format.md) | The externally visible interface controlled by [clause 8](#8-interface-control). |
| [MF-DEV-001](development-environment.md) | The toolchain a build must be reproducible in. |
| [MF-PRC-001](commit-message-workflow.md) | The commit message rule and the hook that enforces it. |
| [MF-PRC-002](git-core-hookspath.md) | How `core.hooksPath` behaves, for when the hook does not run. |

---

## 3 CM management

### 3.1 Roles and authority

| Role | Responsibility | Authority |
|---|---|---|
| Maintainer | Performs CM: applies identifiers, declares baselines, tags releases. | Approves a change into a baseline. |
| Author | Prepares a change, keeps documents in step with it, runs the gates. | None over baselines. |
| Reviewer | Performs the technical review of MF-VVP-001, clause 10. | Blocks a change that does not meet its criteria. |

There is no separate configuration control board. The maintainer is the change authority; for a
change that alters a baselined requirement or the published interface, the decision and its reason
are recorded in the affected document ([MF-SRS-001, Annex B](requirements.md#annex-b--decision-record)
or [MF-IFS-001](question-import-format.md)) rather than in a separate change register.

### 3.2 Tools and infrastructure

| Function | Tool | Notes |
|---|---|---|
| Version control | Git, remote `dimitrije-cuk/memforce` | The single source of truth for every controlled item. |
| Change identification | Git commits and tags | Commit messages follow [MF-PRC-001](commit-message-workflow.md), enforced locally by `.githooks/commit-msg`. |
| Build reproducibility | Gradle wrapper, pinned to 9.4.1 in `gradle/wrapper/gradle-wrapper.properties` | Every machine builds with the same Gradle. |
| Dependency pinning | `gradle/libs.versions.toml` | Every external version appears once, in one file. |
| Line-ending control | `.gitattributes` | `.githooks/**` is checked out with LF so the hook's shebang survives on every platform. |
| Environment setup | `setup-env.ps1` | Installs the SDK packages the build needs and writes `local.properties`, which is not controlled. |

---

## 4 Configuration identification

### 4.1 Configuration items

A configuration item is anything whose change must be controlled and traceable.

| CI class | Items | Identifier | Version carried by |
|---|---|---|---|
| Application source | `app/src/main/**`, `app/src/test/**` | Path in the repository | The commit |
| Build definition | `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/**`, `gradlew`, `gradlew.bat` | Path | The commit |
| Delivered product | The application package built from a tagged commit | `versionName` + `versionCode` | `app/build.gradle.kts` |
| Database schema | The DDL in `MemForceDbHelper.onCreate` | `DATABASE_VERSION` | The constant in that class |
| Interface definition | [MF-IFS-001](question-import-format.md), `docs/schemas/question-set.schema.json`, the template and example files | `formatVersion` | The JSON documents and the schema |
| Requirements | [MF-SRS-001](requirements.md) | `MF-SRS-001` | The document header |
| Design | [MF-SDD-001](design.md) | `MF-SDD-001` | The document header |
| V&V | [MF-VVP-001](verification-and-validation.md) | `MF-VVP-001` | The document header |
| CM | This document | `MF-CMP-001` | The document header |
| Supporting documents | [MF-DEV-001](development-environment.md), [MF-PRC-001](commit-message-workflow.md), [MF-PRC-002](git-core-hookspath.md), `docs/README.md` | `MF-DEV-001`, `MF-PRC-001`, `MF-PRC-002`, `MF-DOC-000` | The document header |
| Design artefacts | `docs/database/database-erd.puml` and its rendered PNG and SVG | Path | The commit; the PlantUML source is authoritative and the renderings are regenerated from it |
| Seed data | `app/src/main/assets/seed/memforce_seed.sql` and `app/src/main/assets/question-sets/**` | Path | The commit |

**Not controlled:** `local.properties`, `.gradle/`, `build/`, IDE files and any downloaded SDK —
all excluded by `.gitignore`. They are machine state, not product; anything that must survive a
fresh clone belongs in a controlled item instead.

### 4.2 Identification schemes

| Scheme | Rule |
|---|---|
| Document identifier | `MF-<TYPE>-<NNN>`, assigned once and never reused. `SRS`, `SDD`, `VVP`, `CMP`, `IFS`, `DEV`, `PRC`, `DOC`. |
| Document version | `major.minor`. Minor for editorial or additive change; major when a baselined statement changes meaning. Stated in the header table of each document. |
| Requirement identifier | `REQ-<AREA>-<NN>`, per [MF-SRS-001, 1.9](requirements.md#19-conventions). Never changed, never reused. |
| Product version | `versionName` follows semantic versioning; `versionCode` increases by one at every release, never decreases. |
| Schema version | `DATABASE_VERSION`, an integer that increases by one whenever the shipped DDL changes. Raising it is subject to [clause 8.2](#82-database-schema). |
| Interface version | `formatVersion` in a question set, per [clause 8.1](#81-question-set-format). |
| Baseline | `BL-<n>`, applied as a Git tag on the commit that constitutes it. |
| Release tag | `v<versionName>`, applied to the commit an application package was built from. |

### 4.3 Baselines

| Baseline | What it fixes | Declared when |
|---|---|---|
| **BL-1 — functional baseline** | [MF-SRS-001](requirements.md) v1.1, [MF-SDD-001](design.md) v1.1, [MF-VVP-001](verification-and-validation.md) v1.1, [MF-IFS-001](question-import-format.md) v1.1, this plan v1.1, and the source they describe. | The documentation set is reviewed and consistent with the source at that commit. |
| **Product baseline** | The application package of a release, together with the exact documents and source it was built from. | A release tag is applied, after the release checklist of [clause 10](#10-release-management) passes. |

A baseline is not a snapshot of intent, it is a snapshot of fact: it may only be declared when the
verification record of [MF-VVP-001, clause 12](verification-and-validation.md#12-reporting-and-records)
exists for that commit.

**No baseline has been declared yet.** The documents listed under BL-1 are at *Draft* status and
carry that status in their headers; BL-1 comes into existence when the tag is applied to the
commit that satisfies the condition above, and the document headers change in the same commit.

---

## 5 Configuration change control

### 5.1 The path a change takes

1. **Identify the impact.** Name the requirements the change affects. A change with no requirement
   behind it is either a defect fix — which must name the anomaly — or a change to the
   requirements, which is itself a controlled change to [MF-SRS-001](requirements.md).
2. **Work on a branch.** `main` always holds a state that builds and passes its gates; work
   happens on a topic branch named for what it does.
3. **Commit in units that make sense on their own**, with a message conforming to
   [MF-PRC-001](commit-message-workflow.md): `type: subject`, with `type` one of `feat`, `fix`,
   `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`. The
   `.githooks/commit-msg` hook rejects anything else; run `setup-hooks.bat` once after cloning so
   that it does.
4. **Keep the documents in step in the same change.** A change to behaviour that leaves
   [MF-SRS-001](requirements.md) describing the old behaviour is an incomplete change, not a
   change plus a documentation task. This rule is what stops the documentation set from decaying
   into fiction.
5. **Pass the gates** of [MF-VVP-001, 7.4](verification-and-validation.md#74-entry-exit-suspension-and-resumption):
   the build compiles, the automated suite passes, the suites covering the changed area pass, and
   a reviewer who is not the author approves.
6. **Merge.** The commit that lands on `main` is the unit of status accounting.

### 5.2 Classification of changes

| Class | Examples | Additional control |
|---|---|---|
| **Interface change** | Anything in [clause 8](#8-interface-control) | Follows the versioning rule of that clause; may not be made silently. |
| **Baselined document change** | A requirement's meaning, a design decision, a V&V criterion | The document version increases; the reason is recorded in the document itself. |
| **Product change** | Source, resources, seed data | The gates of 5.1 apply. |
| **Housekeeping** | Formatting, comments, editorial documentation fixes | The gates of 5.1 apply, minus the requirement impact step. |

### 5.3 Emergency changes

There is no separate emergency path. A fix urgent enough to bypass review is urgent enough to
justify the reviewer being interrupted; a change that lands without review is recorded as an
anomaly against the process in [MF-VVP-001, 9.2](verification-and-validation.md#92-open-anomalies)
and reviewed retrospectively before the next baseline.

### 5.4 Deviations and waivers

A decision to ship with a known deviation from a requirement is recorded as an anomaly with an
explicit disposition in [MF-VVP-001, clause 9](verification-and-validation.md#9-anomaly-management),
naming the requirement, the reason and the condition that would reopen it. Anomalies A-02 to A-10
are the deviations currently accepted, deferred or awaiting a fix; A-01 is recorded there as
closed. A deviation that is not written
down is not a deviation, it is a defect nobody has found yet.

---

## 6 Configuration status accounting

| Question | Answered by |
|---|---|
| What changed, when, and why? | The commit history: one typed subject line per unit of change. |
| What does version *x* of the product contain? | The release tag and the commit it points at. |
| Which requirements exist and at which version? | [MF-SRS-001](requirements.md) header and its history. |
| Which requirements are verified against which build? | The verification record of [MF-VVP-001, clause 12](verification-and-validation.md#12-reporting-and-records). |
| What is known to be wrong? | [MF-VVP-001, 9.2](verification-and-validation.md#92-open-anomalies). |
| What is deferred, and why? | [MF-SRS-001, 1.8](requirements.md#18-apportioning-of-requirements) and the anomaly dispositions. |
| Which external versions are in use? | `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`. |

No separate status database is kept: every question above is answered by a controlled item, and a
second record of the same fact would eventually disagree with the first.

---

## 7 Configuration auditing

Two audits are performed before a release tag is applied. Both are conducted by the maintainer
against the candidate commit, and both are recorded with the baseline.

### 7.1 Functional configuration audit — does it do what was specified?

1. Every requirement in [MF-SRS-001](requirements.md) clause 3 has a passing verification record
   against this build.
2. Every open anomaly is *minor*, or carries a recorded disposition accepted for this release.
3. The mechanical checks pass: every requirement identifier appears exactly once in clause 4 of
   MF-SRS-001; every identifier is covered by a suite in
   [MF-VVP-001, clause 8](verification-and-validation.md#8-test-suites-and-procedures).

### 7.2 Physical configuration audit — is the delivered thing the thing that was built?

1. The application package was built from the tagged commit, with no uncommitted change in the
   working tree.
2. `versionName` and `versionCode` in the package match the tag and the previous release's values
   plus one.
3. `DATABASE_VERSION` matches the DDL described in [MF-SDD-001, 3.5.1](design.md#351-schema).
4. The published schema, template and example files validate against each other and are accepted
   by the built application.
5. The documentation set describes this build: the document headers name the baseline, and no
   document refers to a capability the build does not have.

---

## 8 Interface control

Three interfaces are visible outside a single build and may not change silently.

### 8.1 Question-set format

The format defined by [MF-IFS-001](question-import-format.md) is consumed by files that users and
chatbots produce outside this repository, so a change to it is a change to other people's data.

| Change | Version treatment |
|---|---|
| A new optional field, or a relaxed limit | New minor `formatVersion`; existing files keep working; the importer continues to accept the old version. |
| A removed or renamed field, a new required field, or a changed meaning | New major `formatVersion`; the importer must state clearly which versions it reads (REQ-IMP-30). |
| A change to the JSON Schema | Made in the same commit as the change to MF-IFS-001 and to the parser; the published template and example are updated and must still validate. |

### 8.2 Database schema

`DATABASE_VERSION` and the DDL change together, in one commit, with the ERD source regenerated in
the same commit. Raising the version also adds the migration step for it, so that
[REQ-DB-100](requirements.md#35-logical-database-requirements) holds for an installation already in
use: each step alters the database in place, no step may drop a table holding a user's work, and a
version for which no step is known raises rather than opening a database short of the schema
([MF-SDD-001, 3.5.7](design.md#357-schema-evolution)). Procedure
[MF-VVP-001, 8.3](verification-and-validation.md#83-database-integrity) step 9 is executed at every
such change. This replaces the blocking condition formerly carried by anomaly
[A-01](verification-and-validation.md#92-open-anomalies), closed when the versioned migrations
arrived.

### 8.3 Platform interface

`minSdk`, `targetSdk` and `compileSdk` in `app/build.gradle.kts` define the platform contract
verified by REQ-POR-10.

- Raising `minSdk` removes support for devices that the current release serves; it is a major
  product change and requires the requirement baseline to be updated.
- Raising `targetSdk` changes the platform behaviour applied to the application; the system suites
  are re-executed at the new level before it is released.
- `compileSdk` may be raised on its own, provided the build and the suites pass.

---

## 9 Supplier and third-party control

MemForce has no supplier and no subcontracted work. Its external dependencies are the items below,
which are controlled as if they were suppliers: pinned, upgraded deliberately, and verified after
each upgrade.

| Dependency | Pinned in | Upgrade rule |
|---|---|---|
| Gradle 9.4.1 | `gradle/wrapper/gradle-wrapper.properties` | Upgraded by `gradlew wrapper`, committed as a `build:` change, verified by a clean build. The wrapper properties do not currently carry `distributionSha256Sum`; adding it would make the distribution's integrity verifiable and is the one improvement this clause recommends. |
| Android Gradle Plugin 9.2.1 | `gradle/libs.versions.toml` | Upgrade may change the required build-tools version; [MF-DEV-001](development-environment.md) states how to detect that and keep `setup-env.ps1` in step. |
| AndroidX AppCompat 1.7.0, ConstraintLayout 2.2.1, RecyclerView 1.3.2, Material 1.12.0 | `gradle/libs.versions.toml` | One dependency per change, with the system suites re-executed; a change in appearance is checked in both light and dark themes (REQ-USE-60). |
| JUnit 4.13.2, `org.json` 20240303 (test scope) | `gradle/libs.versions.toml` | Test-only; upgraded when the automated suite is being worked on, never together with a product change. |

No dependency is taken without a version. A floating or dynamic version would make two builds of
the same commit produce different products, which defeats every other control in this plan.

---

## 10 Release management

### 10.1 Release checklist

1. `main` holds the intended content; the working tree is clean.
2. `versionName` and `versionCode` are set in `app/build.gradle.kts`, `versionCode` exactly one
   above the previous release.
3. The automated suite passes; the manual system suites of
   [MF-VVP-001, clause 8](verification-and-validation.md#8-test-suites-and-procedures) pass on
   both platform levels; performance is measured at the reference volume.
4. The audits of [clause 7](#7-configuration-auditing) pass and are recorded.
5. Document headers state the baseline; anomaly dispositions are current.
6. Tag the commit `v<versionName>`, and the baseline `BL-<n>` if a new baseline is declared.
7. Build the release package from the tagged commit and record its identity with the baseline.

### 10.2 Delivery and installation

The deliverable is a single Android application package, installed by the platform's own
installer (REQ-CON-10). There is no server component, no migration script and no post-install
step. A user upgrading in place keeps their database, which is what makes the migration rule of
[clause 8.2](#82-database-schema) the most consequential rule in this plan.

### 10.3 Retirement

Should the product be retired, the final release remains tagged and buildable, and the last
baseline remains the record of what it did. No user data is held anywhere but on the user's own
device, so retirement requires no data disposal on the project's part.

---

## 11 Plan maintenance and tailoring

This plan is reviewed whenever a new class of configuration item appears — a second module, a
build pipeline, a second delivery channel — and at each release baseline.

| # | Provision of IEEE Std 828-2012 | Tailoring | Justification |
|---|---|---|---|
| T-1 | Configuration control board (clause 9) | The maintainer is the change authority; no board convenes. | A board coordinates competing interests; with one team there are none to coordinate. The decisions a board would minute are recorded in the affected documents instead. |
| T-2 | Separate change request and change status records (clauses 9 and 10) | Changes are requested and tracked as commits, branches and anomaly entries. | A parallel register would duplicate the repository history and would drift from it. |
| T-3 | Supplier configuration item control (clause 13) | Reduced to the pinning and upgrade rules of [clause 9](#9-supplier-and-third-party-control). | There is no supplier; the clause is applied to third-party libraries because they present the same risk. |
| T-4 | Formal library structure with promotion levels (clause 8) | Two levels only: topic branch and `main`, plus tags for baselines and releases. | Development, integration and release libraries would be three names for the same repository at this scale. |
| T-5 | CM measurement and process improvement reporting | Not performed as a separate activity. | The measures that matter are those of [MF-VVP-001, clause 11](verification-and-validation.md#11-measures); CM-specific measures would report on a process with one participant. |
