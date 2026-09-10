# MemForce — Software Requirements Specification

| Field | Value |
|---|---|
| Document title | MemForce — Software Requirements Specification |
| Document identifier | MF-SRS-001 |
| Version | 1.0 |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Product | MemForce, version 1.0 (`versionCode` 1, `versionName` "1.0") |
| Information item | Software Requirements Specification (SRS) |
| Conforms to | ISO/IEC/IEEE 29148:2018, clause 9.6 |
| Verified by | [MF-VVP-001](verification-and-validation.md) |
| Realised by | [MF-SDD-001](design.md) |

## Conformance

This document claims **conformance to the information item content provisions** of
ISO/IEC/IEEE 29148:2018 (clause 4.4) for the Software Requirements Specification information
item. Specifically:

- the content of clause 9.6 is produced in full, as shown in the [clause map](#clause-map);
- every requirement follows the requirement construct of clause 5.2.4, the nine characteristics
  of an individual requirement of clause 5.2.5, and the language criteria of clause 5.2.7;
- the requirement set satisfies the five characteristics of a requirement set of clause 5.2.6;
  the audit is recorded in [Annex D](#annex-d--requirement-quality-self-audit);
- tailoring is recorded in [Annex E](#annex-e--tailoring-record), as the normative tailoring
  annex of the standard requires.

No claim of **full conformance** (clause 4.2) is made: the project does not claim conformance to
the requirements-engineering *processes* of ISO/IEC/IEEE 15288 and ISO/IEC/IEEE 12207, and it
does not produce the BRS, StRS and SyRS information items. Those omissions are deliberate and
justified in [Annex E](#annex-e--tailoring-record).

### Clause map

| Section in this document | ISO/IEC/IEEE 29148:2018 subclause |
|---|---|
| [1.1 Purpose](#11-purpose) | 9.6.2 Purpose |
| [1.2 Scope](#12-scope) | 9.6.3 Scope |
| [1.3 Product perspective](#13-product-perspective) | 9.6.4 Product perspective |
| [1.4 Product functions](#14-product-functions) | 9.6.5 Product functions |
| [1.5 User characteristics](#15-user-characteristics) | 9.6.6 User characteristics |
| [1.6 Limitations](#16-limitations) | 9.6.7 Limitations |
| [1.7 Assumptions and dependencies](#17-assumptions-and-dependencies) | 9.6.8 Assumptions and dependencies |
| [1.8 Apportioning of requirements](#18-apportioning-of-requirements) | 9.6.9 Apportioning of requirements |
| [1.9 Conventions](#19-conventions) | 5.2.4, 5.2.7, 5.2.8 (construct, language, attributes) |
| [2 References](#2-references) | 9.6.1 (document identification and references) |
| [3 Specified requirements](#3-specified-requirements) | 9.6.10 Specified requirements |
| [3.1 External interfaces](#31-external-interfaces) | 9.6.11 External interfaces |
| [3.2 Functions](#32-functions) | 9.6.12 Functions |
| [3.3 Usability requirements](#33-usability-requirements) | 9.6.13 Usability requirements |
| [3.4 Performance requirements](#34-performance-requirements) | 9.6.14 Performance requirements |
| [3.5 Logical database requirements](#35-logical-database-requirements) | 9.6.15 Logical database requirements |
| [3.6 Design constraints](#36-design-constraints) | 9.6.16 Design constraints |
| [3.7 Standards compliance](#37-standards-compliance) | 9.6.17 Standards compliance |
| [3.8 Software system attributes](#38-software-system-attributes) | 9.6.18 Software system attributes |
| [4 Verification](#4-verification) | 9.6.19 Verification |
| [5 Supporting information](#5-supporting-information) | 9.6.20 Supporting information |

---

## 1 Introduction

### 1.1 Purpose

This document specifies the requirements for **MemForce**, a standalone Android application for
capturing, classifying and retrieving quiz questions. It states what MemForce must do and the
constraints it must respect, in a form that can be designed to, implemented against, and
verified.

It is the authoritative statement of required behaviour for the product. It is written for the
engineers who build and maintain MemForce, for whoever verifies it, and for anyone integrating
with its published file format. Design decisions are recorded in [MF-SDD-001](design.md); the
evidence that these requirements hold is recorded in [MF-VVP-001](verification-and-validation.md).

### 1.2 Scope

The software product specified here is **MemForce**, version 1.0.

MemForce shall let a person authenticate against a local account, maintain a shared body of quiz
questions classified by tag, import prepared question sets from a JSON file, and retrieve
questions by text pattern, by tag, or by both. All data is held in a SQLite database in
application-private storage on the device.

MemForce shall **not** deliver study sessions, scoring, scheduling, content sharing, export, or
any network-based capability. Those capabilities are listed in
[1.8 Apportioning of requirements](#18-apportioning-of-requirements).

The benefit sought is retention of learning material: a learner captures questions once,
classifies them so they can be found again, and can load a whole topic at once instead of typing
it in.

### 1.3 Product perspective

MemForce is **self-contained**. It is not a component of a larger system, it has no server-side
counterpart, and it exchanges no data with any external system at run time.

```text
┌───────────────────────────── Android device ──────────────────────────────┐
│                                                                           │
│   User ──▶ MemForce ──┬──▶ SQLite database  (application-private storage)  │
│                       ├──▶ Preferences      (signed-in account identity)   │
│                       └──◀ Question-set file (read-only, user-selected)    │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
                             no network interface
```

Its interfaces are the touchscreen user interface, the platform SQLite persistence interface, the
platform preferences store that holds the signed-in identity, and the platform document-selection
interface through which the user offers a question-set file for import. All are specified in
[3.1 External interfaces](#31-external-interfaces).

### 1.4 Product functions

MemForce provides five groups of function.

- **Authentication and session.** A person signs in with a user name and a password. An unknown
  user name creates an account, so first use and subsequent use follow the same path. The signed-in
  identity persists until the person signs out.
- **Question management.** A question — its text, an optional answer, and any number of tags — is
  created, changed, listed and deleted. Questions are shared: every account sees every question.
- **Tag management.** A tag is the multi-valued classification of a question (*algebra*,
  *space*, *exam*). Tags are created, renamed, searched and deleted, and are shared by all
  accounts.
- **Search and filtering.** Questions are retrieved by text pattern, by tag, or by both together;
  tags are retrieved by name pattern. Patterns support the `%` and `_` wildcards.
- **Question-set import.** A JSON file conforming to [MF-IFS-001](question-import-format.md) is
  validated as a whole and then merged into the stored questions and tags in one transaction.

### 1.5 User characteristics

There is a single class of user: a **learner** who captures and organises study material. No
administrator, moderator, or privileged role exists, and no function is restricted to a subset of
users.

Users are assumed to be familiar with everyday Android applications. No knowledge of databases or
of JSON is assumed, except that a user who types a wildcard character is assumed to intend its
wildcard meaning, and that a user who imports a file is assumed to have obtained it from the
documented template.

Several people may share one device by holding separate accounts. In version 1.0 accounts
separate sign-in only: questions and tags are common to all accounts
([REQ-QST-60](#322-questions), [REQ-TAG-60](#323-tags)).

### 1.6 Limitations

- **Single device.** All data lives on one device. There is no synchronisation, backup transport,
  export, or transfer between devices or installations.
- **Offline only.** No function may depend on network connectivity, and the application declares
  no permission of any kind.
- **Local persistence only.** Persistence uses SQLite through platform APIs; no other storage
  technology may be introduced for application data.
- **Not a security boundary between accounts.** Accounts separate sign-in, not data. A person
  with physical access to an unlocked device can sign in as any account by supplying that
  account's password, and an unknown user name silently creates an account. MemForce therefore
  protects stored credentials ([3.8.1 Security](#381-security)) but does not claim to protect one
  account's content from another account's user.
- **Content is text.** A question, an answer and a tag are text. No media, formatting, or
  attachments are stored.
- **ASCII case folding.** Wherever this document says that a comparison ignores letter case, it
  means the case of ASCII letters. Case folding of other alphabets is not provided in version 1.0
  ([1.8](#18-apportioning-of-requirements)), so two names differing only in the case of a
  non-ASCII letter are treated as different names.

### 1.7 Assumptions and dependencies

| # | Assumption or dependency | Consequence if it does not hold |
|---|---|---|
| A-1 | The target device or emulator runs Android API level 24 or higher. | [REQ-POR-10](#383-portability-and-maintainability) fails; the platform baseline must be renegotiated. |
| A-2 | The platform provides SQLite with foreign-key enforcement available per connection. | [REQ-DB-50](#35-logical-database-requirements) cannot be met by the database engine and the cascade rules must be enforced in application code. |
| A-3 | Free storage is sufficient for the database, assumed under 50 MB at the volumes of [3.4](#34-performance-requirements). | Write operations fail; [REQ-REL-30](#382-reliability-and-availability) governs the outcome. |
| A-4 | The **reference device** for timing is a device or emulator with at least 2 GB RAM and four cores at 1.4 GHz or better, running the minimum supported API level. | The numeric limits in [3.4](#34-performance-requirements) are not verifiable as stated. |
| A-5 | The platform provides a document-selection interface that returns a readable stream for a user-chosen file. | [REQ-IMP-10](#325-question-set-import) cannot be met and import must be withdrawn. |
| A-6 | One person uses the application at a time; no second process accesses the database. | Concurrency requirements would have to be added. |
| A-7 | Question-set files are produced from the template of [MF-IFS-001](question-import-format.md) and are of the order of hundreds of questions, not tens of thousands. | The file-size limit of [REQ-IMP-90](#325-question-set-import) rejects legitimate files and must be raised. |

### 1.8 Apportioning of requirements

The following are **outside version 1.0** and are recorded here so that their absence is a
decision rather than an omission. They may be introduced in a later version without invalidating
this specification.

| Deferred capability | Reason |
|---|---|
| Study sessions, question presentation, answer checking, scoring | MemForce 1.0 manages material; it does not drill it. |
| Spaced-repetition scheduling | Depends on study sessions. |
| Export of questions or tags to a file | The import format is defined for one direction in 1.0; export would make the format a two-way contract and is deferred until the format stabilises ([MF-IFS-001](question-import-format.md), *Extensibility*). |
| Personal collections of questions owned by one account | 1.0 shares all content between accounts ([REQ-QST-60](#322-questions)); ownership would require an owning key on content and a per-account filter throughout. |
| Synchronisation, cloud backup, multi-device use | Excluded by the standalone constraint ([REQ-CON-10](#36-design-constraints)). |
| Media (images, audio) in questions | Limitation of [1.6](#16-limitations); the format reserves room for it. |
| Localisation, internationalised collation and non-ASCII case folding | Wildcard matching, ordering and every case-insensitive comparison are specified for ASCII letters only ([1.6](#16-limitations), [REQ-SRCH-70](#324-search-and-filtering)). |
| Password change, password recovery, account deletion | No user-facing account management exists in 1.0; the data effect that deleting an account would have is nonetheless stated in [3.5.3](#353-deletion-rules). |
| Multiple tags in one filter, saved searches | 1.0 filters by at most one tag at a time ([REQ-SRCH-20](#324-search-and-filtering)). |

### 1.9 Conventions

**Requirement construct.** Every requirement is written as *[condition] subject + verb phrase +
object + [constraint]*, following clause 5.2.4 of ISO/IEC/IEEE 29148:2018. The subject is "the
application" (MemForce as a whole) or a named entity of the data model.

**Normative keywords** follow clause 5.2.7 of the same standard:

| Keyword | Meaning |
|---|---|
| **shall** | A binding requirement. Its verification entry in [clause 4](#4-verification) must pass. |
| **should** | A recommendation. Deviating is permitted but must be justified in the design. |
| **may** | A permission. Neither doing nor omitting it constitutes a defect. |
| **will** | A statement of fact or intent about the environment — never a requirement. |

Superlatives, subjective language ("user-friendly", "fast"), and open-ended terms ("etc.",
"as appropriate") are not used; every quality statement is expressed as a measurable criterion.

**Identifiers.** Each requirement carries the identifier `REQ-<AREA>-<NN>`:

| Area | Meaning | Area | Meaning |
|---|---|---|---|
| `EXT` | External interfaces | `PERF` | Performance |
| `AUTH` | Authentication and session | `DB` | Logical database |
| `QST` | Questions | `CON` | Design constraints |
| `TAG` | Tags | `STD` | Standards compliance |
| `SRCH` | Search and filtering | `SEC` | Security |
| `IMP` | Question-set import | `REL` | Reliability and availability |
| `USE` | Usability | `POR` | Portability and maintainability |

Numbers advance in steps of ten so that a later insertion never renumbers an existing
requirement. In line with clause 5.2.8.2, **an identifier is never changed and never reused**: a
withdrawn requirement is marked *Obsolete* and kept.

**Attributes.** Each requirement carries identification, type, stakeholder priority, source, and
a verification method; rationale is given for each group. The attributes *version number*,
*owner*, *risk* and *difficulty* offered by clause 5.2.8.2 are not carried — see
[Annex E](#annex-e--tailoring-record).

| Attribute | Values |
|---|---|
| Type | `F` functional · `I` interface · `D` data · `Q` quality · `C` constraint |
| Priority | **H** — the product does not meet its purpose without it · **M** — materially improves the product; may be traded against schedule · **L** — desirable refinement |
| Source | `SN-nn`, a stakeholder need from [Annex A.1](#a1-stakeholder-needs); or *derived*, with a decision reference `D-nn` from [Annex B](#annex-b--decision-record) |
| Verification | `I` inspection · `A` analysis · `D` demonstration · `T` test — defined in [4.1](#41-verification-methods) |

**Terminology** follows ISO/IEC/IEEE 24765:2017; product terms are defined in
[Annex C](#annex-c--acronyms-abbreviations-and-terms).

---

## 2 References

### 2.1 Normative references

| Reference | Use in this document |
|---|---|
| ISO/IEC/IEEE 29148:2018, *Systems and software engineering — Life cycle processes — Requirements engineering* | Document structure (clause 9.6), requirement construct and characteristics (5.2.4–5.2.7), attributes (5.2.8), conformance (4.4), tailoring (Annex C). |
| [MF-IFS-001 — Question import format](question-import-format.md) | The controlled definition of the question-set file read by [3.2.5](#325-question-set-import). |

### 2.2 Informative references

| Reference | Relevance |
|---|---|
| ISO/IEC/IEEE 24765:2017, *Vocabulary* | Meaning of *requirement*, *verification*, *validation*, *traceability*. |
| IEEE Std 1012-2016, *System, Software, and Hardware Verification and Validation* | Source of the inspection / analysis / demonstration / test method vocabulary used in [clause 4](#4-verification). |
| ISO/IEC/IEEE 29119-1:2013, *Software testing — Concepts and definitions* | Testing concepts underlying the acceptance criteria of [clause 4](#4-verification). |
| IEEE Std 828-2012, *Configuration Management in Systems and Software Engineering* | Control of this document as a configuration item — see [MF-CMP-001](configuration-management.md). |
| ISO/IEC TR 29110-5-1-2:2011, *Very Small Entities — Basic profile* | The right-sizing rationale for the tailoring in [Annex E](#annex-e--tailoring-record). |
| IEEE Std 830-1998, *Recommended Practice for Software Requirements Specifications* | The superseded SRS practice from which clause 9.6 descends; cited for lineage only. |

> The referenced standards are copyrighted by their publishers. This document cites clause
> numbers and paraphrases structure; it reproduces no substantive text.

---

## 3 Specified requirements

This clause states every requirement placed on MemForce. Each requirement is uniquely identified,
singular, and verifiable; its verification method and acceptance criteria are given in the
correspondingly numbered subclause of [clause 4](#4-verification), as clause 9.6.19 of
ISO/IEC/IEEE 29148:2018 recommends.

The functional requirements of [3.2](#32-functions) are organised **by feature**. Clause 8.5.2 of
the standard lists feature organisation among the accepted schemes, and it is the one that
matches MemForce: the product is a set of management capabilities over distinct entities, with no
operating modes, no user classes, and no stimulus-driven behaviour.

Column conventions are defined in [1.9 Conventions](#19-conventions).

### 3.1 External interfaces

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-EXT-10 | The application shall provide a sign-in screen, a main menu, a question list screen, a question editor, a tag list screen, and a tag editor. | I | H | SN-01, SN-02, SN-05 |
| REQ-EXT-20 | Once a user is signed in, the application shall provide navigation from the main menu to the question list and to the tag list, and a return path from each screen to the screen that opened it, without repeating sign-in. | I | H | SN-01, D-05 |
| REQ-EXT-30 | The question list screen and the tag list screen shall each display the items they manage as a scrollable list and shall expose the create, edit, delete and search actions defined for that entity in [3.2](#32-functions). | I | H | SN-01, SN-03 |
| REQ-EXT-40 | Every search input field shall accept the characters `%` and `_` as typed input and shall pass them unaltered to the search function as pattern characters. | I | H | SN-03 |
| REQ-EXT-50 | The application shall provide no communications interface: it shall neither transmit nor receive data over any network interface of the device, and it shall declare no permission in its manifest. | I | H | SN-06 |
| REQ-EXT-60 | The application's only interface for question, tag and account records shall be a single SQLite database file located in application-private storage on the device; the only application data held outside that file shall be the retained signed-in identity of [REQ-AUTH-70](#321-authentication-and-session). | I | H | SN-06, SN-07 |
| REQ-EXT-70 | The application shall obtain a question-set file through the platform document-selection interface, shall open it for reading only, and shall access no other file of the device file system. | I | H | SN-04, D-06 |

*Rationale.* REQ-EXT-20 makes reachability verifiable: without it, "the app has screens" would
not state that every capability is available after a single sign-in. REQ-EXT-40 exists because
the wildcard behaviour of [3.2.4](#324-search-and-filtering) is only observable if the input
field passes the characters through untouched. REQ-EXT-50 turns the standalone property into a
property that can be checked on the delivered package rather than by reading code. REQ-EXT-70
bounds file access to what the user explicitly offers, which is what makes the absence of a
storage permission possible.

### 3.2 Functions

#### 3.2.1 Authentication and session

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-AUTH-10 | The application shall provide a sign-in function that accepts a user name and a password entered by the user. | F | H | SN-05 |
| REQ-AUTH-20 | When the submitted user name matches no stored account, the application shall create an account from the submitted user name and password and shall grant access. | F | H | SN-05, D-01 |
| REQ-AUTH-30 | When the submitted user name matches a stored account and the submitted password matches that account's stored credential, the application shall grant access. | F | H | SN-05 |
| REQ-AUTH-40 | When the submitted user name matches a stored account and the submitted password does not match that account's stored credential, the application shall deny access and shall display a message on the password field stating that the password is wrong for that user. | F | H | SN-05 |
| REQ-AUTH-50 | When the user name or the password field is empty, the application shall reject the sign-in attempt, shall mark the empty field as required, and shall create no account. | F | H | derived, D-02 |
| REQ-AUTH-60 | The application shall treat user names as equal when they differ only by letter case, so that a second account with the same name in different case cannot be created. | F | H | derived, D-02 |
| REQ-AUTH-70 | The application shall retain the identity of the signed-in account across restarts of the application until the user signs out, and shall present the signed-in user name on the main menu. | F | M | SN-05, D-03 |
| REQ-AUTH-80 | The application shall provide a sign-out action that discards the retained identity and returns to the sign-in screen. | F | H | SN-05, D-03 |

*Rationale.* Sign-in in MemForce is registration as well as authentication (decision D-01): a
learner who opens the application for the first time must not be stopped by an account-creation
step, and there is no administrator to create accounts. REQ-AUTH-50 and REQ-AUTH-60 close the two
inputs that would otherwise be undefined — empty fields and case-only differences — and without
them a mistyped capital would silently create a second account. REQ-AUTH-70 and REQ-AUTH-80 make
the session an explicit, testable state rather than an implementation side effect. How the
credential is stored is a security attribute and is specified in [3.8.1](#381-security).

#### 3.2.2 Questions

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-QST-10 | The application shall allow a user to create a question consisting of question text, an optional answer text, and any number of tags, including none. | F | H | SN-01, SN-02 |
| REQ-QST-20 | The application shall reject the creation or modification of a question whose question text is empty, and shall mark the question text field as required. | F | H | derived, D-02 |
| REQ-QST-30 | The application shall allow a user to modify the question text, the answer text and the tag assignments of an existing question. | F | H | SN-01 |
| REQ-QST-40 | The application shall allow a user to delete a question, and shall delete it only after the user confirms the deletion. | F | H | SN-01, D-04 |
| REQ-QST-50 | The application shall display the stored questions as a list ordered by question text in ascending order, compared without regard to letter case, each entry showing its question text and the names of its tags. | F | H | SN-01, SN-03 |
| REQ-QST-60 | Every question shall be visible to, and modifiable by, every account, irrespective of which account created it. | F | H | SN-05 |
| REQ-QST-70 | The application shall allow a user to assign any of the existing tags to a question and to withdraw any assignment, and a tag shall apply to a question at most once. | F | H | SN-02 |

*Rationale.* An answer is optional because a question captured during a lecture is often written
before its answer is known, and forcing a placeholder answer would corrupt the data; the import
format makes the same choice for the same reason. REQ-QST-50 fixes the order and the content of
the list so that "the list shows the questions" is verifiable, and so that a user who has just
edited a question can find it again in a predictable place.

#### 3.2.3 Tags

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-TAG-10 | The application shall allow a user to create a tag identified by a name. | F | H | SN-02 |
| REQ-TAG-20 | The application shall reject the creation or renaming of a tag whose name is empty or whose name matches an existing tag name when compared without regard to letter case, and shall display a message stating which of the two applies. | F | H | derived, D-02 |
| REQ-TAG-30 | The application shall allow a user to change the name of an existing tag. | F | H | SN-02 |
| REQ-TAG-40 | The application shall allow a user to delete a tag, and shall delete it only after the user confirms a message that names the tag and states that it will be removed from every question that carries it. | F | H | SN-02, D-04 |
| REQ-TAG-50 | The application shall display the stored tags as a list ordered by name in ascending order, compared without regard to letter case. | F | H | SN-02, SN-03 |
| REQ-TAG-60 | Every tag shall be visible to, and modifiable by, every account, irrespective of which account created it. | F | H | SN-05 |

*Rationale.* Tag names are the vocabulary of the whole product: two tags differing only in case
would split one concept across two filters and make every tag search ambiguous, so REQ-TAG-20
forbids them. REQ-TAG-40 requires the confirmation to state the consequence, because deleting a
tag changes questions the user is not looking at.

#### 3.2.4 Search and filtering

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-SRCH-10 | The application shall retrieve the questions whose question text matches a pattern entered by the user. | F | H | SN-03 |
| REQ-SRCH-20 | The application shall retrieve the questions that carry a tag chosen by the user from the stored tags. | F | H | SN-03 |
| REQ-SRCH-30 | When both a text pattern and a tag are active, the application shall retrieve only the questions that satisfy both criteria. | F | H | SN-03, D-07 |
| REQ-SRCH-40 | When a search criterion is not given, the application shall treat that criterion as satisfied by every item, so that an empty text field and no chosen tag together retrieve every stored question. | F | H | derived, D-07 |
| REQ-SRCH-50 | The application shall retrieve the tags whose name matches a pattern entered by the user, under the same rules as REQ-SRCH-10 and REQ-SRCH-40. | F | H | SN-03 |
| REQ-SRCH-60 | The application shall interpret `%` in a search pattern as zero or more characters and `_` as exactly one character, at any position in the pattern. | F | H | SN-03 |
| REQ-SRCH-70 | The application shall match search patterns without regard to the letter case of ASCII letters. | F | M | derived, D-08 |
| REQ-SRCH-80 | The application shall update the displayed result after each change to a search criterion, without a separate confirming action by the user. | F | M | derived, D-09 |

*Rationale.* Wildcards are exposed to the user deliberately (decision D-08): the alternative —
escaping `%` and `_` so they match literally — would remove the only means of substring search
the product offers, and a tag or question text containing a literal `%` is not a case worth
optimising for. REQ-SRCH-40 makes the empty state definite: an empty search is a request to see
everything, not a request that matches nothing. REQ-SRCH-30 fixes the combination as
conjunction, because a disjunction would widen the result as the user adds criteria, which is the
opposite of what a filter is for.

#### 3.2.5 Question-set import

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-IMP-10 | The application shall import a question set from a file that the user selects through the platform document-selection interface, offered from the question list screen. | F | H | SN-04 |
| REQ-IMP-20 | The application shall validate the whole file against [MF-IFS-001](question-import-format.md) before it writes anything, and when the file violates that format the application shall write nothing and shall report the violations it found, displaying at least the first ten of them and, where more were found, how many remain. | F | H | SN-04, D-10 |
| REQ-IMP-30 | The application shall accept only a question set whose declared format version is one it supports, and shall otherwise report the declared version and the supported version. | F | H | SN-04 |
| REQ-IMP-40 | Before writing, the application shall display the number of questions that would be created, the number of stored questions the file matches and would update, and the number of tags that would be created, and shall write only after the user confirms. | F | H | SN-04, D-10 |
| REQ-IMP-50 | For each tag named in the file that matches no stored tag when compared without regard to letter case, the application shall create a tag; for each tag that matches a stored tag, it shall use the stored tag. | F | H | SN-04 |
| REQ-IMP-60 | When the question text in the file matches a stored question without regard to letter case, the application shall add the file's tags to the stored question rather than store a second question, shall keep the stored answer, and shall set the answer from the file only when the stored question has no answer. | F | H | SN-04, D-11 |
| REQ-IMP-70 | When one file repeats the same question text, the application shall store one question carrying the union of the tags of every repetition and the first answer given for it. | F | H | SN-04, D-11 |
| REQ-IMP-80 | The application shall apply an import as a single transaction, so that after a failure at any point the database holds exactly what it held before the import began. | F | H | SN-04, D-10 |
| REQ-IMP-90 | The application shall reject a file larger than 1 MiB, a file it cannot read, and a file that is not valid JSON, in each case reporting the reason and writing nothing. | F | H | derived, D-12 |
| REQ-IMP-100 | The application shall report, after a completed import, how many questions were added, how many stored questions the file matched and updated, and how many tags were created. | F | M | SN-04 |

*Rationale.* Import is the one function that changes many records from a single action, so it is
specified defensively: validate everything first (REQ-IMP-20), show the effect and ask
(REQ-IMP-40), write atomically (REQ-IMP-80), and report what happened (REQ-IMP-100). The merge
rules of REQ-IMP-60 and REQ-IMP-70 exist because the expected source of these files is a chatbot
working from a template, which repeats questions across files and within a file; without merging,
a second import of an improved set would double the library. Keeping the stored answer makes
import safe to repeat: an import can add classification, never overwrite work.

### 3.3 Usability requirements

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-USE-10 | The application shall make every function of [3.2](#32-functions) reachable from the main menu in at most two user actions. | Q | M | SN-01, D-05 |
| REQ-USE-20 | The application shall require a confirmation before deleting a question or a tag, and the confirmation shall identify the item to be deleted. | Q | H | derived, D-04 |
| REQ-USE-30 | When the application rejects an entry, it shall display a message on the field that caused the rejection and shall retain the values the user has already entered. | Q | H | derived |
| REQ-USE-40 | When a list is empty because nothing matches the current search, the application shall display a message that says so, in place of an empty area. | Q | M | derived |
| REQ-USE-50 | After a create, an edit, a delete or an import, the application shall display the affected list with the change applied. | Q | H | derived |
| REQ-USE-60 | The application shall follow the device's light or dark appearance setting, and shall be legible under both without further user action. | Q | M | SN-08 |
| REQ-USE-70 | Each screen that offers a pattern search shall display the meaning of the `%` and `_` wildcards. | Q | M | SN-03, D-08 |

*Rationale.* These are the smallest set of usability properties that make the product safe and
predictable, and each is verifiable — unlike "the interface shall be user-friendly", which
clause 5.2.7 forbids. REQ-USE-70 is the counterpart of REQ-SRCH-60: exposing wildcards to users
is only defensible if the application tells them the two characters mean something.

### 3.4 Performance requirements

The limits below apply on the **reference device** of assumption A-4, with a database populated
to the **reference volume**: 2 000 questions, 300 tags, 8 000 question-tag assignments, and 5
accounts.

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-PERF-10 | At the reference volume, the application shall display the result of any search or filter operation of [3.2.4](#324-search-and-filtering) within 1 s of the criterion changing. | Q | M | derived |
| REQ-PERF-20 | At the reference volume, the application shall complete and display the result of the creation, modification or deletion of a single question or tag within 500 ms of the action being confirmed. | Q | M | derived |
| REQ-PERF-30 | At the reference volume, the application shall accept user input on its first screen within 3 s of being launched. | Q | L | derived |
| REQ-PERF-40 | The application shall perform credential derivation, file reading and import writing outside the user-interface thread, so that no such operation blocks the user interface. | Q | H | derived, D-13 |
| REQ-PERF-50 | At the reference volume, the application shall complete the import of a question set of 200 questions within 10 s of the user confirming it. | Q | M | derived |
| REQ-PERF-60 | The application shall support at least the reference volume without loss of function. | Q | M | derived |

*Rationale.* The numbers are stated because clause 5.2.5 requires verifiability and clause 5.2.7
forbids terms such as "responsive". The reference volume is set roughly two orders of magnitude
above the seeded starting data and an order of magnitude above realistic single-learner use, so
the limits remain feasible while still meaningful. REQ-PERF-40 is stated as a requirement rather
than left to design because credential derivation is deliberately expensive
([REQ-SEC-10](#381-security)) and would otherwise make the sign-in screen appear to hang.

### 3.5 Logical database requirements

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-DB-10 | All persistent question, tag and account records shall be held in a single SQLite database stored in application-private storage on the device, the retained signed-in identity of [REQ-AUTH-70](#321-authentication-and-session) excepted. | D | H | SN-06, SN-07 |
| REQ-DB-20 | The database shall represent the entities Users, Questions, Tags and QuestionTags with at least the attributes and constraints listed in [3.5.1](#351-entities-and-attributes). | D | H | SN-01, SN-02, SN-05 |
| REQ-DB-30 | Each of Users, Questions and Tags shall have an integer primary key that is unique within its entity and that is not reused after a deletion. | D | H | derived |
| REQ-DB-40 | QuestionTags shall relate Questions to Tags as a many-to-many relation whose primary key is the pair of referenced keys, so that a tag applies to a question at most once. | D | H | SN-02 |
| REQ-DB-50 | The database shall enforce referential integrity for every foreign key on every connection it opens, so that no record may reference a non-existent record. | D | H | derived, D-14 |
| REQ-DB-60 | Deleting a tag shall delete every QuestionTags record that references it, and shall delete no question. | D | H | SN-02 |
| REQ-DB-70 | Deleting a question shall delete every QuestionTags record that references it, and shall delete no tag. | D | H | SN-01 |
| REQ-DB-80 | The database shall reject a second Users record whose name equals an existing name, and a second Tags record whose name equals an existing name, in both cases comparing without regard to letter case. | D | H | derived, D-02 |
| REQ-DB-90 | Data committed by a completed operation shall remain available after the application is closed and reopened and after the device is restarted. | D | H | derived |
| REQ-DB-100 | The database shall carry a schema version number, and when that number increases the application shall convert an existing database to the new version without loss of stored questions, tags, assignments or accounts. | D | H | derived, D-15 |

#### 3.5.1 Entities and attributes

The minimum content of each entity. The design may add attributes; it may not remove these or
weaken the stated constraints. The realised schema is given in [MF-SDD-001](design.md), clause
3.5.

| Entity | Attribute | Type | Constraint |
|---|---|---|---|
| **Users** | identifier | INTEGER | Primary key, not reused (REQ-DB-30) |
| | name | TEXT | Not null; unique without regard to letter case (REQ-DB-80) |
| | password hash | TEXT | Not null; derived credential per [REQ-SEC-10](#381-security) |
| | salt | TEXT | Not null; per-account random value per [REQ-SEC-10](#381-security) |
| **Questions** | identifier | INTEGER | Primary key, not reused (REQ-DB-30) |
| | question text | TEXT | Not null; the non-empty rule of REQ-QST-20 is enforced by the application, not by the database |
| | answer text | TEXT | Nullable; absent when the question has no recorded answer (REQ-QST-10) |
| **Tags** | identifier | INTEGER | Primary key, not reused (REQ-DB-30) |
| | name | TEXT | Not null; unique without regard to letter case (REQ-DB-80) |
| **QuestionTags** | question identifier | INTEGER | Not null; references Questions; deletion cascades (REQ-DB-70) |
| | tag identifier | INTEGER | Not null; references Tags; deletion cascades (REQ-DB-60) |
| | — | — | Primary key is the pair (question identifier, tag identifier) (REQ-DB-40) |

#### 3.5.2 Relationships

```text
Users                Questions  >────  QuestionTags  ────<  Tags
(sign-in only,       (shared)          (M:N, cascading)     (shared)
 no content link)
```

Users has no relationship to content: version 1.0 shares all questions and tags between accounts
([REQ-QST-60](#322-questions), [REQ-TAG-60](#323-tags)), so no content record carries an owning
account. Introducing personal collections would add that link — see
[1.8](#18-apportioning-of-requirements).

#### 3.5.3 Deletion rules

| Deleting a … | Effect | Rule |
|---|---|---|
| Tag | Its assignments to questions are removed; the questions survive | REQ-DB-60 |
| Question | Its tag assignments are removed; the tags survive | REQ-DB-70 |
| Account | Nothing else is affected, because no content references an account | [3.5.2](#352-relationships) |

### 3.6 Design constraints

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-CON-10 | MemForce shall be a standalone Android application that executes on a single device and requires no companion installation. | C | H | SN-06 |
| REQ-CON-20 | Every function specified in [3.2](#32-functions) shall be available while the device has no network connectivity. | C | H | SN-06 |
| REQ-CON-30 | The application shall depend on no server-side component and on no external service at run time. | C | H | SN-06 |
| REQ-CON-40 | Persistence shall be implemented with SQLite through interfaces provided by the Android platform. | C | H | SN-06, D-14 |
| REQ-CON-50 | The application shall request no Android runtime permission and shall declare no permission in its manifest. | C | H | SN-06, SN-07 |

*Rationale.* These are constraints on the *solution*, kept separate from the functional
requirements so that [3.2](#32-functions) states need rather than implementation, as clause 5.2.7
requires. REQ-CON-50 is what makes the import interface of REQ-EXT-70 a design obligation: the
file must arrive through the platform's document-selection interface, because the application may
not ask for storage access.

### 3.7 Standards compliance

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-STD-10 | The application shall observe the activity lifecycle and permission model of the Android platform versions it supports, so that it retains no unsaved user data when the platform stops it and requests no permission it does not use. | C | M | derived |
| REQ-STD-20 | This specification shall conform to the SRS content provisions of ISO/IEC/IEEE 29148:2018 clause 9.6, with tailoring recorded in [Annex E](#annex-e--tailoring-record). | C | M | derived |
| REQ-STD-30 | A question-set file shall be accepted only when it conforms to [MF-IFS-001](question-import-format.md) and to the JSON Schema published with it. | C | H | SN-04 |

### 3.8 Software system attributes

#### 3.8.1 Security

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-SEC-10 | The application shall store an account's credential only as a one-way value derived with PBKDF2-HMAC-SHA1 over the password and a per-account random salt of at least 128 bits obtained from a cryptographically secure random source, using at least 100 000 iterations and producing a key of at least 256 bits. | Q | H | SN-07, D-16 |
| REQ-SEC-20 | The application shall verify a password by deriving the value again from the submitted password and the stored salt and comparing it with the stored value in constant time, and shall at no time write a password to the database, to a log, or to the screen in recoverable form. | Q | H | SN-07, D-16 |
| REQ-SEC-30 | The database file shall be created in application-private storage, so that no other application on the device can read it. | Q | H | SN-07 |
| REQ-SEC-40 | The application shall declare no network permission in its manifest. | Q | H | SN-06 |
| REQ-SEC-50 | The record that retains the signed-in identity between application starts shall hold the account identifier and name only, and shall hold no credential or derived credential. | Q | H | SN-07, D-03 |
| REQ-SEC-60 | The application shall exclude its database and its retained-identity record from platform backup transports, so that account and content data do not leave the device without an explicit user action. | Q | M | SN-06, SN-07, D-17 |

*Rationale.* Storing a password as typed would expose every account on the device from a single
file, and passwords are commonly reused elsewhere; decision D-16 therefore fixes a salted,
iterated derivation with parameters chosen for the platform baseline. REQ-SEC-60 exists because
"the data never leaves the device" is not achieved by declaring no network permission alone: the
platform's own backup transport can copy application data off the device unless the application
opts out.

#### 3.8.2 Reliability and availability

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-REL-10 | The application shall not terminate abnormally in response to empty input, input longer than a field is expected to carry, or input containing pattern or quotation characters, in any input field of any screen. | Q | H | derived |
| REQ-REL-20 | An operation that changes more than one database record shall be applied as a single transaction; if any part fails, the database shall be left as it was before the operation began. | Q | H | derived |
| REQ-REL-30 | When a database operation fails, the application shall report the failure to the user and shall remain usable. | Q | H | derived |
| REQ-REL-40 | After an abnormal termination, the application shall on the next launch present the data as of the last completed operation. | Q | M | derived |

*Rationale.* REQ-REL-20 is what makes the cascading deletions of [3.5.3](#353-deletion-rules) and
the import of [REQ-IMP-80](#325-question-set-import) safe: both touch several tables, and a
partial write would leave exactly the orphaned records that referential integrity exists to
prevent.

#### 3.8.3 Portability and maintainability

| ID | Requirement | Type | Pri | Source |
|---|---|---|---|---|
| REQ-POR-10 | The application shall install and run on Android API level 24 and on every later level up to and including the level it declares as its target. | Q | M | derived, D-18 |
| REQ-POR-20 | The application shall depend on no device capability beyond a touchscreen and local storage. | Q | M | derived, D-18 |
| REQ-POR-30 | The application shall build from a clean checkout of the repository with the toolchain named in [MF-DEV-001](development-environment.md), without manual steps beyond the documented setup script. | Q | H | derived, D-19 |
| REQ-POR-40 | The logic that parses and merges a question set shall depend on no Android platform type, so that it can be executed and tested on a plain Java virtual machine. | Q | M | derived, D-19 |

*Rationale.* A platform baseline is stated because "runs on Android" is otherwise unverifiable
and portability cannot be assessed. REQ-POR-30 and REQ-POR-40 are the two maintainability
properties that are externally observable — a reproducible build and a testable core — rather
than design prescriptions such as module size, which clause 9.6.18 warns against placing in an
SRS.

---

## 4 Verification

This clause gives the approach planned to qualify MemForce against
[clause 3](#3-specified-requirements). It is laid out **in parallel** with clause 3 — subclause
4.*n* verifies subclause 3.*n* — as recommended by clause 9.6.19 of ISO/IEC/IEEE 29148:2018.
Every requirement identifier of clause 3 appears exactly once below, with the method used and the
criteria that decide pass or fail. The procedures that execute these criteria, the current
results, and any open deviation are held in [MF-VVP-001](verification-and-validation.md).

### 4.1 Verification methods

| Code | Method | Applied when |
|---|---|---|
| **I** | **Inspection** — examination of the product, its manifest, its schema or this document against a stated criterion, without executing the function. | The property is static and visible without running the software. |
| **A** | **Analysis** — reasoning over the design, the schema or measured data to establish that the requirement holds. | Exhaustive execution is impractical, or a limit must be shown to hold across a range. |
| **D** | **Demonstration** — operating the application and observing the outcome, without instrumentation. | The requirement concerns observable interaction. |
| **T** | **Test** — execution against a defined input with a predetermined expected result, recorded and repeatable. | The requirement is functional or data-related and the outcome is exactly predictable. |

The method vocabulary follows IEEE Std 1012-2016. Verification is complete for version 1.0 when
every entry in 4.2 to 4.9 has passed against the same build; validation is complete when that
build is exercised end to end against the product functions of [1.4](#14-product-functions).

### 4.2 External interfaces

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-EXT-10 | D | All six screens are opened in one session, each showing what it manages. |
| REQ-EXT-20 | D | From the main menu, the question list and the tag list are each opened and left again; the sign-in screen does not reappear. |
| REQ-EXT-30 | D | On each list screen the list scrolls when items exceed the viewport, and the create, edit, delete and search actions are all present. |
| REQ-EXT-40 | T | Typing `h_st%` into each search field results in that exact string being used as the pattern; no character is stripped, escaped or reordered. |
| REQ-EXT-50 | I | The manifest declares no `uses-permission` element; the application's own code contains no networking call; with the device in flight mode every function of 3.2 completes normally. |
| REQ-EXT-60 | I | Exactly one SQLite database file exists, under the application's private data directory; no question, tag or account data is found in any other store. |
| REQ-EXT-70 | I | Import is started only through the platform document-selection interface; the returned stream is opened for reading; no other file path is opened by the application. |

### 4.3 Functions

#### 4.3.1 Authentication and session

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-AUTH-10 | D | The sign-in screen accepts text in a user name field and in a password field and submits both on the sign-in action. |
| REQ-AUTH-20 | T | Given no account `alice`, when `alice`/`pw1` is submitted, then access is granted and exactly one new account record for `alice` exists. |
| REQ-AUTH-30 | T | Given account `alice` created with `pw1`, when `alice`/`pw1` is submitted, then access is granted and no new account record is created. |
| REQ-AUTH-40 | T | Given account `alice` created with `pw1`, when `alice`/`wrong` is submitted, then access is denied, the wrong-password message appears on the password field, and the account record is unchanged. |
| REQ-AUTH-50 | T | For each of (empty user name, empty password, both empty): access is denied, the empty field is marked as required, and the Users table is unchanged. |
| REQ-AUTH-60 | T | Given account `alice`, when `ALICE`/`pw2` is submitted, then no second account is created; the attempt is treated as a sign-in for `alice` and fails on the password. |
| REQ-AUTH-70 | T | After signing in as `alice` and stopping the application, the next launch opens the main menu showing `alice` without asking for a password. |
| REQ-AUTH-80 | D | The sign-out action returns to the sign-in screen; the next launch asks for a user name and password. |

#### 4.3.2 Questions

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-QST-10 | T | A question created with text only, and a question created with text, answer and three tags, are both stored with exactly the values entered. |
| REQ-QST-20 | T | Saving with an empty question text is refused on create and on edit, the field is marked required, and no record is written. |
| REQ-QST-30 | T | Changing text, answer and tag selection of a stored question stores exactly the new values and leaves the question's identifier unchanged. |
| REQ-QST-40 | T | The delete action raises a confirmation; declining leaves the question stored; accepting removes it. |
| REQ-QST-50 | T | With questions `apple`, `Banana`, `cherry` stored, the list shows them in that order, and each entry displays the names of the tags assigned to it. |
| REQ-QST-60 | T | A question created while signed in as `alice` is visible and editable after signing in as `bob`. |
| REQ-QST-70 | T | Two tags are assigned to a question and one is withdrawn; the assignment table holds exactly one row for that question; re-selecting an assigned tag does not create a second row. |

#### 4.3.3 Tags

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-TAG-10 | T | A tag created with the name `algebra` is stored and appears in the tag list. |
| REQ-TAG-20 | T | Saving an empty name is refused with the required-field message; saving `ALGEBRA` when `algebra` exists is refused with the name-taken message; in both cases nothing is written. |
| REQ-TAG-30 | T | Renaming `algebra` to `linear-algebra` stores the new name, keeps the tag's identifier, and preserves its assignments. |
| REQ-TAG-40 | T | The delete action raises a confirmation naming the tag and stating the consequence; declining leaves the tag stored; accepting removes it. |
| REQ-TAG-50 | T | With tags `basics`, `Exam`, `space` stored, the list shows them in that order. |
| REQ-TAG-60 | T | A tag created while signed in as `alice` is visible and editable after signing in as `bob`. |

#### 4.3.4 Search and filtering

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-SRCH-10 | T | With the reference data loaded, a text pattern returns exactly the questions whose text matches it and no others. |
| REQ-SRCH-20 | T | Choosing a tag returns exactly the questions carrying that tag, including questions that also carry other tags. |
| REQ-SRCH-30 | T | A pattern matching six questions combined with a tag carried by four of them returns exactly the questions in both sets. |
| REQ-SRCH-40 | T | An empty text field with no chosen tag returns every stored question; an empty text field with a chosen tag returns every question carrying that tag. |
| REQ-SRCH-50 | T | A pattern applied to the tag list returns exactly the matching tags; an empty pattern returns every tag. |
| REQ-SRCH-60 | T | The patterns `po%`, `%ta`, `%sto%`, `_br%` and `%__a` each return exactly the members of the prepared data set that satisfy the stated wildcard rule. |
| REQ-SRCH-70 | T | The patterns `PO%` and `po%` return the same questions. |
| REQ-SRCH-80 | D | Typing successive characters into a search field narrows the displayed list after each character, with no confirming action. |

#### 4.3.5 Question-set import

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-IMP-10 | D | The import action on the question list screen opens the platform document picker and accepts a JSON file chosen there. |
| REQ-IMP-20 | T | A file that breaks the format is rejected with the violations reported, and the question, tag and assignment tables are byte-for-byte unchanged; a file with several violations reports them together — at least the first ten, and a count of any remainder — rather than only the first. |
| REQ-IMP-30 | T | A file declaring an unsupported format version is rejected with a message naming the declared and the supported version. |
| REQ-IMP-40 | T | For a file of 10 questions of which 3 already exist and which names 2 unknown tags, the confirmation shows 7, 3 and 2; declining leaves the database unchanged. |
| REQ-IMP-50 | T | Importing a file naming the existing tag `Algebra` and the unknown tag `topology` creates exactly one tag, and the imported questions reference the existing `algebra` row. |
| REQ-IMP-60 | T | Importing a question whose text matches a stored question in different case adds the file's tags to the stored question, creates no second question, keeps the stored answer, and fills the answer only where the stored question had none. |
| REQ-IMP-70 | T | A file repeating one question text three times with different tags produces one question carrying the union of those tags and the first answer given. |
| REQ-IMP-80 | T | An import interrupted by an induced failure after the first write leaves the database exactly as it was before the import. |
| REQ-IMP-90 | T | A file of 2 MiB, a file whose stream cannot be opened, and a file holding invalid JSON are each rejected with the corresponding message and no write. |
| REQ-IMP-100 | T | After a completed import the reported counts equal the counts confirmed beforehand, and the reported numbers of created questions and created tags equal the actual change in the tables. |

### 4.4 Usability requirements

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-USE-10 | A | Each function of 3.2 is traced to a path from the main menu; no path exceeds two user actions. |
| REQ-USE-20 | T | For a question and for a tag: the delete action raises a confirmation that identifies the item; declining leaves it stored; accepting removes it. |
| REQ-USE-30 | D | A rejected entry shows a message on the offending field, and the values already entered are still present in the form. |
| REQ-USE-40 | D | A search matching nothing shows the no-results message in place of the list. |
| REQ-USE-50 | D | After each of a create, an edit, a delete and an import, the affected list is displayed with the change applied. |
| REQ-USE-60 | D | With the device set to light and then to dark appearance, every screen renders with the corresponding theme and every label remains legible. |
| REQ-USE-70 | I | The question list and tag list screens each display the wildcard help text. |

### 4.5 Performance requirements

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-PERF-10 | T | At the reference volume on the reference device, ten searches of each kind (text, tag, combined) each display their result within 1 s of the criterion changing. |
| REQ-PERF-20 | T | At the reference volume, ten single-item creations, ten modifications and ten deletions each complete within 500 ms of confirmation. |
| REQ-PERF-30 | T | From a cold start at the reference volume, the first screen accepts input within 3 s, averaged over five launches. |
| REQ-PERF-40 | I | Credential derivation, file reading and import writing are each executed off the user-interface thread; during a sign-in and during an import the interface continues to redraw. |
| REQ-PERF-50 | T | A 200-question set imports within 10 s of confirmation at the reference volume. |
| REQ-PERF-60 | A | With the database populated to the reference volume, every function of 3.2 is exercised without error, and the measurements above are taken on that populated database. |

### 4.6 Logical database requirements

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-DB-10 | I | Exactly one SQLite database file holds all question, tag and account data; no such data is found in preferences, plain files, or any other store. |
| REQ-DB-20 | I | The schema declares all four entities, each with at least the attributes and constraints of [3.5.1](#351-entities-and-attributes). |
| REQ-DB-30 | T | Each of the three principal entities declares an integer primary key; a record inserted after a deletion does not reuse the deleted key. |
| REQ-DB-40 | I | QuestionTags declares both foreign keys and a composite primary key over the pair. |
| REQ-DB-50 | T | On a freshly opened connection, an insert naming a non-existent parent key is rejected by the database. |
| REQ-DB-60 | T | Deleting a tag carried by three questions removes exactly those three assignment rows and leaves all three questions stored. |
| REQ-DB-70 | T | Deleting a question carrying two tags removes exactly those two assignment rows and leaves both tags stored. |
| REQ-DB-80 | T | Inserting a user name and a tag name that differ from an existing one only by case is rejected by the database, not only by the user interface. |
| REQ-DB-90 | T | Records created before the application is closed are present after it is reopened, and again after the device is restarted. |
| REQ-DB-100 | T | A database created by the previous schema version is opened by the current build; every previously stored question, tag, assignment and account is readable afterwards. |

### 4.7 Design constraints

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-CON-10 | I | The deliverable is a single Android application package that installs and runs with no companion installation. |
| REQ-CON-20 | D | With the device in flight mode, every function of 3.2 completes normally. |
| REQ-CON-30 | I | No endpoint address and no remote-service dependency exists in the application's configuration, dependencies or code. |
| REQ-CON-40 | I | Persistence is implemented over the platform SQLite interfaces; no other persistence engine appears among the dependencies. |
| REQ-CON-50 | I | The manifest declares no permission, and no runtime permission request appears in the code. |

### 4.8 Standards compliance

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-STD-10 | A | Each screen is reviewed for lifecycle handling: the application restores its screen state after a configuration change and after process death, and each declared component is traced to a function that uses it. |
| REQ-STD-20 | I | This document is checked against the [clause map](#clause-map): every subclause of 9.6.2–9.6.20 has content, and [Annex E](#annex-e--tailoring-record) states every deviation. |
| REQ-STD-30 | T | The published example and template files validate against the published JSON Schema and are accepted by the application; a file that the schema rejects is also rejected by the application. |

### 4.9 Software system attributes

| ID | Method | Acceptance criteria |
|---|---|---|
| REQ-SEC-10 | I | The Users table holds a derived value and a salt and no password column; two accounts created with the same password hold different salts and different derived values; the derivation uses PBKDF2-HMAC-SHA1 with at least 100 000 iterations, a key of at least 256 bits and a salt of at least 128 bits from a secure random source. |
| REQ-SEC-20 | T | After a successful and a failed sign-in, neither the database, the application logs, nor the screen contains the submitted password; comparison of derived values is performed by a constant-time routine; sign-in still succeeds with the correct password. |
| REQ-SEC-30 | I | The database file resides in the application's private data directory and is not readable by another application. |
| REQ-SEC-40 | I | The manifest contains no network permission. |
| REQ-SEC-50 | I | The retained-identity record holds an account identifier and a name only. |
| REQ-SEC-60 | I | The manifest excludes application data from platform backup, or declares rules that exclude the database file and the retained-identity record. |
| REQ-REL-10 | T | Each input field is submitted empty, with 10 000 characters, and with `%`, `_` and a quotation character; the application reports an error or accepts the input, and in no case terminates abnormally. |
| REQ-REL-20 | T | A question creation, a question update and an import are each interrupted by an induced failure after the first write; on the next read every affected record is either wholly present or wholly absent, never a mixture. |
| REQ-REL-30 | T | With the database made unwritable, a create action reports a failure to the user and the application remains operable. |
| REQ-REL-40 | T | After the process is killed while idle, the next launch shows exactly the data of the last completed operation. |
| REQ-POR-10 | T | The application installs and passes the functional checks of 4.3 on an emulator at API level 24 and on one at the declared target level. |
| REQ-POR-20 | I | The manifest declares no required hardware feature other than a touchscreen. |
| REQ-POR-30 | T | On a machine holding only the documented toolchain, a clean checkout builds and its unit tests run after the documented setup script, with no further manual step. |
| REQ-POR-40 | I | The parsing and merging classes import no Android platform type; their unit tests execute on a plain Java virtual machine. |

---

## 5 Supporting information

The material in clause 5 and in the annexes is **not itself a requirement**. It records how the
requirements were derived, how they trace, and how they are controlled, as clause 9.6.20 of
ISO/IEC/IEEE 29148:2018 asks an SRS to state explicitly. Where an annex appears to state
behaviour, the binding statement is the requirement it cites in
[clause 3](#3-specified-requirements).

### 5.1 The problem being solved

A learner accumulates study material faster than they can organise it. Questions are captured
once and then cannot be found again, because there is no way to say what a question is *about*,
and because typing a whole topic in by hand is slow enough that it does not happen. MemForce
addresses both: any number of tags per question give retrieval more than one handle, pattern
search over text and tags makes a growing library stay usable, and a question set prepared
outside the application — typically by asking a chatbot to fill in the published template — loads
in one confirmed, reversible step.

### 5.2 Baseline and change control

This document is a configuration item under [MF-CMP-001](configuration-management.md).

- Version 1.0 is **proposed for** baseline **BL-1**; it becomes baselined when that tag is
  applied under [MF-CMP-001, 4.3](configuration-management.md#43-baselines). Until then its
  status is *Draft* and requirements may change without a change record. Afterwards, a change to
  any requirement is a change to a baselined item and follows the change control process of
  MF-CMP-001, clause 5.
- A change to a requirement shall be reflected in the same change in its verification entry in
  [clause 4](#4-verification), in [MF-VVP-001](verification-and-validation.md), and in the design
  elements that [MF-SDD-001](design.md) traces to it.
- Requirement identifiers are never reassigned. A withdrawn requirement remains in its table,
  marked *Obsolete*, with the version in which it was withdrawn.

### 5.3 Requirement summary

| Group | Clause | Count |
|---|---|---|
| External interfaces (`EXT`) | 3.1 | 7 |
| Authentication and session (`AUTH`) | 3.2.1 | 8 |
| Questions (`QST`) | 3.2.2 | 7 |
| Tags (`TAG`) | 3.2.3 | 6 |
| Search and filtering (`SRCH`) | 3.2.4 | 8 |
| Question-set import (`IMP`) | 3.2.5 | 10 |
| Usability (`USE`) | 3.3 | 7 |
| Performance (`PERF`) | 3.4 | 6 |
| Logical database (`DB`) | 3.5 | 10 |
| Design constraints (`CON`) | 3.6 | 5 |
| Standards compliance (`STD`) | 3.7 | 3 |
| Security (`SEC`) | 3.8.1 | 6 |
| Reliability and availability (`REL`) | 3.8.2 | 4 |
| Portability and maintainability (`POR`) | 3.8.3 | 4 |
| **Total** | | **91** |

Priority distribution: 71 High, 19 Medium, 1 Low. The High requirements are those without which
the product does not meet the purpose of [1.1](#11-purpose); together they form the minimum
acceptable product.

---

## Annex A — Traceability

Traceability is maintained in both directions without a separate matrix that could drift out of
step: the **Source** column of each requirement table carries the backward trace to a stakeholder
need or a decision, and the identifier shared between clause 3 and clause 4 carries the forward
trace to verification. [MF-SDD-001](design.md) extends the chain to design elements and
[MF-VVP-001](verification-and-validation.md) to executed procedures.

### A.1 Stakeholder needs

The needs that MemForce exists to meet. Version 1.0 does not produce a separate Stakeholder
Requirements Specification ([Annex E](#annex-e--tailoring-record), T-1); the needs are recorded
here so that every requirement has a stated origin.

| ID | Stakeholder need | Covered by |
|---|---|---|
| SN-01 | A learner captures a question and its answer once, and can change or remove it later. | REQ-QST-10, REQ-QST-20, REQ-QST-30, REQ-QST-40, REQ-QST-50, REQ-EXT-10, REQ-EXT-20, REQ-EXT-30, REQ-USE-10 |
| SN-02 | A learner classifies a question along as many dimensions as the material needs. | REQ-QST-10, REQ-QST-70, REQ-TAG-10, REQ-TAG-20, REQ-TAG-30, REQ-TAG-40, REQ-TAG-50, REQ-DB-40, REQ-DB-60 |
| SN-03 | A learner finds a question again by what it says or by what it is about, in a library that keeps growing. | REQ-SRCH-10, REQ-SRCH-20, REQ-SRCH-30, REQ-SRCH-50, REQ-SRCH-60, REQ-EXT-40, REQ-USE-70, REQ-QST-50, REQ-TAG-50 |
| SN-04 | A learner loads a whole topic prepared outside the application instead of typing it in. | REQ-IMP-10 … REQ-IMP-100, REQ-EXT-70, REQ-STD-30 |
| SN-05 | Several people share one device, each signing in under their own name, with no server involved. | REQ-AUTH-10, REQ-AUTH-20, REQ-AUTH-30, REQ-AUTH-40, REQ-AUTH-70, REQ-AUTH-80, REQ-QST-60, REQ-TAG-60, REQ-DB-20 |
| SN-06 | The application works with no connectivity and sends nothing anywhere. | REQ-CON-10, REQ-CON-20, REQ-CON-30, REQ-CON-40, REQ-CON-50, REQ-EXT-50, REQ-EXT-60, REQ-DB-10, REQ-SEC-40, REQ-SEC-60 |
| SN-07 | A password stored on the device cannot be recovered from the device's storage. | REQ-SEC-10, REQ-SEC-20, REQ-SEC-30, REQ-SEC-50, REQ-SEC-60, REQ-EXT-60, REQ-DB-10 |
| SN-08 | The application is comfortable to read in the same lighting conditions as the rest of the device. | REQ-USE-60 |

### A.2 Forward trace — requirements to verification

Each requirement `REQ-X-nn` in clause 3.*n* is verified by the entry bearing the same identifier
in clause 4, in the subclause that mirrors its own:

| Requirements | Verified in |
|---|---|
| 3.1 External interfaces | [4.2](#42-external-interfaces) |
| 3.2.1 – 3.2.5 Functions | [4.3.1](#431-authentication-and-session) – [4.3.5](#435-question-set-import) |
| 3.3 Usability | [4.4](#44-usability-requirements) |
| 3.4 Performance | [4.5](#45-performance-requirements) |
| 3.5 Logical database | [4.6](#46-logical-database-requirements) |
| 3.6 Design constraints | [4.7](#47-design-constraints) |
| 3.7 Standards compliance | [4.8](#48-standards-compliance) |
| 3.8 Software system attributes | [4.9](#49-software-system-attributes) |

### A.3 Derived requirements

Requirements that do not come directly from a stakeholder need, added because the set would
otherwise not be **complete** in the sense of clause 5.2.6.

| Requirement | Why it was added |
|---|---|
| REQ-AUTH-50, REQ-AUTH-60, REQ-QST-20, REQ-TAG-20, REQ-DB-80 | No need statement defines behaviour for empty input or for names differing only by case; without these, a mistyped capital creates a second account or a duplicate tag. |
| REQ-SRCH-40, REQ-SRCH-70, REQ-SRCH-80 | The empty-criteria case, letter case, and when results refresh are otherwise undefined. |
| REQ-IMP-90 | A file interface must state what it refuses, or every malformed file becomes an unhandled failure. |
| REQ-USE-10 … REQ-USE-50 | Usability needs a verifiable form; these five are the properties that make the product safe and predictable. |
| REQ-PERF-10 … REQ-PERF-60 | Without numbers, "usable at size" cannot be verified. |
| REQ-DB-30, REQ-DB-50, REQ-DB-90, REQ-DB-100 | Key stability, integrity enforcement, durability and schema evolution are implied by "the data is kept" but never stated. |
| REQ-STD-10, REQ-STD-20 | Platform conformance and this document's own conformance are otherwise unstated. |
| REQ-REL-10 … REQ-REL-40 | Robustness and atomicity are what make the integrity rules of 3.5.3 hold in practice. |
| REQ-POR-10 … REQ-POR-40 | No platform baseline, build reproducibility or testability property is stated by any need. |

---

## Annex B — Decision record

Points that were open when the requirements were written, closed here. Clause 5.2.6 requires a
requirement set to carry no `TBD`, `TBS` or `TBR`. A decision may be revisited; doing so is a
change to this document under [5.2](#52-baseline-and-change-control).

| ID | Question | Decision | Rejected alternatives and why | Realised by |
|---|---|---|---|---|
| **D-01** | Should account creation be a separate step from sign-in? | **No.** An unknown user name creates the account and signs in. | *Separate registration screen* — adds a step to first use for no gain in a product with no administrator, no email verification and no account recovery. The cost is that a mistyped name creates an account, which REQ-AUTH-60 bounds to genuinely different names. | REQ-AUTH-20 |
| **D-02** | What are the validity and uniqueness rules for names and text? | User name unique; tag name unique; question text and tag name not empty; all comparisons ignore letter case. | *Allowing duplicates* — two tags named `Algebra` and `algebra` split one concept and make every tag filter ambiguous. *Case-sensitive uniqueness* — the same failure with extra steps. | REQ-AUTH-50, REQ-AUTH-60, REQ-QST-20, REQ-TAG-20, REQ-DB-80 |
| **D-03** | Should the signed-in identity survive an application restart? | **Yes**, until the user signs out, held in application-private preferences. | *Sign in on every launch* — a learner who opens the application a dozen times a day types a password each time, for no security gain on a device that already has a lock screen. *Holding a credential in the record* — never necessary, so the record holds an identifier and a name only (REQ-SEC-50). | REQ-AUTH-70, REQ-AUTH-80, REQ-SEC-50 |
| **D-04** | Is a deletion confirmed? | **Yes**, for questions and tags, and the tag confirmation states the effect on questions. | *Immediate deletion with undo* — needs an undo stack across screens; a confirmation is cheaper and sufficient at this scale. | REQ-QST-40, REQ-TAG-40, REQ-USE-20 |
| **D-05** | How are the capabilities arranged on screen? | A **main menu** leading to a question area and a tag area, each with its list and editor. | *One screen with tabs* — the two areas share no state, and separate activities keep the back stack meaningful. | REQ-EXT-10, REQ-EXT-20, REQ-USE-10 |
| **D-06** | How does a file reach the application without a storage permission? | Through the **platform document-selection interface**, which grants read access to the one file the user picks. | *Reading a fixed directory* — needs a storage permission and a file manager, and would put MemForce in charge of a folder it does not own. | REQ-EXT-70, REQ-CON-50 |
| **D-07** | How do a text pattern and a tag filter combine? | **Conjunction**: a question must satisfy every active criterion; an inactive criterion restricts nothing. | *Disjunction* — adding a criterion would widen the result, which is the opposite of filtering. | REQ-SRCH-30, REQ-SRCH-40 |
| **D-08** | Are `%` and `_` wildcards or literal characters? | **Wildcards**, exposed to the user, with on-screen help. | *Escaping them to match literally* — removes the product's only substring search. The residual cost is that a literal `%` cannot be searched for; no stored content is expected to contain one. | REQ-SRCH-60, REQ-EXT-40, REQ-USE-70 |
| **D-09** | When is a search executed? | **On every change** to a criterion. | *On an explicit search action* — an extra action per refinement; at the reference volume the query is well inside the limit of REQ-PERF-10. | REQ-SRCH-80 |
| **D-10** | How much of a question-set file is applied when part of it is wrong? | **None of it.** Validate the whole file, show the effect, then write in one transaction. | *Import what parses* — leaves the user with a partly loaded topic and no way to tell which questions are missing. | REQ-IMP-20, REQ-IMP-40, REQ-IMP-80 |
| **D-11** | What happens when an imported question already exists? | **Merge**: keep one question, add the file's tags, keep the stored answer, fill an absent answer. | *Store a second copy* — a re-import doubles the library. *Overwrite the stored answer* — an import would silently destroy work the user did by hand. | REQ-IMP-60, REQ-IMP-70 |
| **D-12** | What bounds a file the application will read? | **1 MiB**, valid JSON, readable stream. | *No bound* — a user-chosen file is untrusted input; without a bound a large file exhausts memory on the minimum device. 1 MiB holds several thousand questions, well beyond assumption A-7. | REQ-IMP-90 |
| **D-13** | Where do expensive operations run? | **Off the user-interface thread**: credential derivation, file reading, import writing. | *On the interface thread* — the deliberate cost of REQ-SEC-10 would freeze sign-in and risk a platform "not responding" termination. | REQ-PERF-40 |
| **D-14** | Which persistence technology, and how is integrity enforced? | **SQLite through the platform APIs**, with foreign-key enforcement switched on for every connection. | *A document store or plain files* — would put referential integrity in application code. *Relying on the SQLite default* — foreign keys are off by default, which would make every cascade rule inert. | REQ-CON-40, REQ-DB-50 |
| **D-15** | What happens to stored data when the schema changes? | The schema carries a version, and an increase converts existing data. | *Recreating the schema on upgrade* — discards everything the user has entered, which for this product is the entire value of the installation. | REQ-DB-100 |
| **D-16** | How is a password stored? | A **salted PBKDF2-HMAC-SHA1 value**, 100 000 iterations, 256-bit key, 128-bit per-account salt. | *Plaintext* — one readable file exposes every account, and passwords are commonly reused elsewhere. *Unsalted hash* — identical passwords yield identical values and fall to precomputed tables. *PBKDF2-HMAC-SHA256* — preferable, but not guaranteed present at API level 24 (assumption A-1); SHA-1 inside PBKDF2 is not affected by the collision weaknesses that rule it out for signatures. | REQ-SEC-10, REQ-SEC-20 |
| **D-17** | May the platform back up application data off the device? | **No.** Backup of the database and the identity record is excluded. | *Allowing platform backup* — copies questions and credential material to a cloud transport, which contradicts SN-06 even though the application itself never opens a socket. | REQ-SEC-60 |
| **D-18** | What is the platform baseline? | **API level 24** minimum, up to the declared target; touchscreen and local storage only. | *A higher minimum* — excludes usable devices for no needed API. *No stated baseline* — "runs on Android" is unverifiable. | REQ-POR-10, REQ-POR-20 |
| **D-19** | What makes the product maintainable in a verifiable way? | A **reproducible build** from a clean checkout, and **platform-free parsing and merging logic**. | *Metrics such as module size or complexity limits* — design prescriptions that clause 9.6.18 warns against placing in an SRS, and weak predictors at this scale. | REQ-POR-30, REQ-POR-40 |

---

## Annex C — Acronyms, abbreviations, and terms

| Term | Meaning |
|---|---|
| **Account** | A user name and credential pair stored by MemForce; the unit of sign-in. Not a boundary between users' content — see [1.6](#16-limitations). |
| **Answer** | The optional text recorded against a question. |
| **API level** | The Android platform version identifier. |
| **Assignment** | The relation between one question and one tag; a QuestionTags record. |
| **Import** | Loading a question set file into the stored questions and tags. |
| **M:N** | A many-to-many relationship between two entities. |
| **PBKDF2** | Password-Based Key Derivation Function 2 — the iterated derivation used for stored credentials. |
| **Question** | The unit of study material: a question text, an optional answer text, and any number of tags. |
| **Question set** | A JSON file holding a batch of questions and tags, defined by [MF-IFS-001](question-import-format.md). |
| **Reference device** | The device class against which the limits of [3.4](#34-performance-requirements) are measured — assumption A-4. |
| **Reference volume** | The dataset size against which those limits are measured — defined in [3.4](#34-performance-requirements). |
| **Session** | The state in which an account is signed in; retained until sign-out (REQ-AUTH-70). |
| **Tag** | A named, multi-valued classification of a question, for example *algebra*. Shared by all accounts. |
| **TBD / TBS / TBR** | To Be Defined / Specified / Resolved — placeholders that clause 5.2.6 forbids in a complete requirement set. |
| **V&V** | Verification and validation. |
| **Wildcard** | A pattern character: `%` matches zero or more characters, `_` matches exactly one. |

Terms not defined here carry the meaning given in ISO/IEC/IEEE 24765:2017.

---

## Annex D — Requirement quality self-audit

### D.1 Characteristics of each individual requirement (clause 5.2.5)

| Characteristic | How it is satisfied | How it was checked |
|---|---|---|
| **Necessary** | Every requirement traces to a stakeholder need in [A.1](#a1-stakeholder-needs) or appears in [A.3](#a3-derived-requirements) with the gap it closes. Removing any one leaves a capability, constraint or quality unmet. | Each requirement was matched against A.1 or A.3; none was left unexplained. |
| **Appropriate** | Requirements state what MemForce must do, at product level. Solution choices are confined to [3.6](#36-design-constraints) and to the decisions of [Annex B](#annex-b--decision-record). | Each statement was read for design content; realisation detail was moved to [MF-SDD-001](design.md). |
| **Unambiguous** | One reading each. Open points — how filters combine, letter case, merge behaviour on import, wildcard meaning — are closed in [Annex B](#annex-b--decision-record). | Every conditional and every enumeration was resolved into a definite statement. |
| **Complete** | Each requirement is understandable on its own; where it depends on another it names it by identifier rather than implying it. | Cross-references were made explicit links. |
| **Singular** | One capability, constraint or quality per requirement; grouped actions such as "add, edit, delete" were split into separate requirements. | Statements containing a conjunction of *actions* were split; enumerations of *fields* within one action were kept (REQ-QST-30). |
| **Feasible** | Every requirement is achievable with the platform APIs on the baseline of assumption A-1, and every numeric limit sits well above measured behaviour at the seeded volume. | Each non-functional limit was checked against the reference device of assumption A-4. |
| **Verifiable** | Each has exactly one entry in [clause 4](#4-verification) with a method and pass criteria. Unmeasurable terms are absent. | Clause 4 was built by walking clause 3 in order; the counts agree at 91. |
| **Correct** | Each requirement is an accurate statement of the need it cites; every decision that shaped it is recorded in [Annex B](#annex-b--decision-record). | Each need was re-read against the requirements claiming it. |
| **Conforming** | All requirements follow the construct of clause 5.2.4 and the keywords of clause 5.2.7, in the fixed table form of [1.9](#19-conventions). | Every statement uses *shall* with an explicit subject. |

### D.2 Characteristics of the requirement set (clause 5.2.6)

| Characteristic | How it is satisfied |
|---|---|
| **Complete** | [A.1](#a1-stakeholder-needs) shows every stated need covered, and the set contains **no TBD, TBS or TBR**: every open point is closed in [Annex B](#annex-b--decision-record). Capabilities deliberately excluded are listed in [1.8](#18-apportioning-of-requirements) rather than left silent. |
| **Consistent** | Identifiers are unique; no two requirements overlap or conflict. Terminology is fixed in [Annex C](#annex-c--acronyms-abbreviations-and-terms) and used identically throughout — *account*, *question*, *tag*, *assignment*, *question set* have one meaning each. Units are uniform (milliseconds and seconds for time, counts for volume, bytes for file size). |
| **Feasible** | The set is deliverable on the stated platform with the stated toolchain; the 71 High requirements form the minimum product, and the 20 Medium and Low requirements are the trade space if schedule pressure arises. |
| **Comprehensible** | The document follows the clause structure of the standard, functional requirements are grouped by the feature a user recognises, and each group carries a rationale note explaining why it says what it says. |
| **Able to be validated** | Satisfying the set produces exactly the application described in [1.4](#14-product-functions). Clause 4 gives the evidence for each requirement; end-to-end use against 1.4 gives the validation. |

---

## Annex E — Tailoring record

ISO/IEC/IEEE 29148:2018 permits tailoring and requires it to be recorded. MemForce is developed
by a very small team — the situation the ISO/IEC 29110 Very Small Entity profiles address by
right-sizing the mainstream life-cycle standards. The tailoring below follows that principle: the
content that makes requirements usable is kept in full; the apparatus that exists to coordinate
several organisations is omitted.

| # | Provision | Tailoring | Justification | Risk accepted |
|---|---|---|---|---|
| T-1 | Information items of clause 7 (BRS, StRS, SyRS, SRS) | Only the **SRS** is produced. Stakeholder needs are recorded in [A.1](#a1-stakeholder-needs). | There is one stakeholder group and no acquirer to negotiate with; the product is software only, so a system-level specification would restate the software one. | A later change of stakeholder would find the needs stated compactly rather than argued in full; A.1 is then the item to expand first. |
| T-2 | Requirement attributes of clause 5.2.8.2 | Identification, type, priority, source and verification method are carried; *version number*, *owner*, *risk* and *difficulty* are not. | One owner, and per-requirement versions duplicate the document version held under [MF-CMP-001](configuration-management.md). Risk and difficulty inform planning, which is not this document's purpose. | Requirement-level history is only recoverable from the repository history of this file. |
| T-3 | Requirements management measurement (clause 6.5) | Volatility is not measured; changes are visible in the document history. | At 91 requirements under one owner, the measure would cost more than it informs. | Requirement churn is not quantified; if the set grows past a few hundred, this should be revisited. |
| T-4 | Concept of operations and operational concept annexes | Not produced as separate items; the operational context is given in [1.3](#13-product-perspective), [1.4](#14-product-functions) and [5.1](#51-the-problem-being-solved). | A single-user application on one device has no operational environment beyond the device itself. | None material at this scale. |
| T-5 | Formal review and approval records | Approval is recorded by the baseline label in [MF-CMP-001](configuration-management.md) rather than by a signature page. | No separate acquirer or quality organisation exists to sign. | Approval authority and date are traceable through the repository history only. |
