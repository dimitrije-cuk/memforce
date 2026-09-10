# MemForce documentation

| Field | Value |
|---|---|
| Document title | MemForce — Documentation index |
| Document identifier | MF-DOC-000 |
| Version | 1.0 |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |

The MemForce documentation set is organised as **information items**: each document has one
purpose, one identifier, and one standard whose content provisions it follows. Nothing is stated
twice — a fact lives in exactly one document, and the others link to it.

## The set

| Document | ID | Answers | Follows |
|---|---|---|---|
| [Software Requirements Specification](requirements.md) | MF-SRS-001 | What must the product do, and how will each obligation be judged? | ISO/IEC/IEEE 29148:2018, clause 9.6 |
| [Software Design Description](design.md) | MF-SDD-001 | How is it built, and why that way? | IEEE Std 1016-2009, clauses 4–5 |
| [Verification and Validation Plan](verification-and-validation.md) | MF-VVP-001 | How is it shown to work, what is tested, and what is known to be wrong? | IEEE Std 1012-2016 (clause 12), ISO/IEC/IEEE 29119-1:2013, IEEE Std 1028-2008, IEEE Std 1044-2009 |
| [Configuration Management Plan](configuration-management.md) | MF-CMP-001 | What is controlled, how does it change, and how is a release proved to match it? | IEEE Std 828-2012, Annex D |
| [Question import format](question-import-format.md) | MF-IFS-001 | What exactly is a question-set file? | Interface specification; JSON Schema draft-07 |
| [Development environment](development-environment.md) | MF-DEV-001 | How is a machine made able to build the product? | — |
| [Commit message workflow](commit-message-workflow.md) | MF-PRC-001 | How is a change recorded? | Procedure under MF-CMP-001, clause 5 |
| [Git `core.hooksPath`](git-core-hookspath.md) | MF-PRC-002 | Why does or does not a hook run? | Background note for MF-PRC-001 |

Supporting artefacts: the schema, template and example of the import format under
[schemas/](schemas) and [templates/](templates), and the entity-relationship diagram under
[database/](database) — PlantUML source with generated PNG and SVG.

## How the documents relate

```text
   stakeholder needs                 MF-SRS-001, Annex A.1
            │
            ▼
   requirements  ─────────────────▶  MF-SRS-001  clause 3   (what)
            │                              │
            │                              ▼
            │                        MF-SRS-001  clause 4   (how it is judged)
            ▼                              │
   design    ─────────────────────▶  MF-SDD-001              (how it is built)
            │                              │
            ▼                              ▼
   evidence  ─────────────────────▶  MF-VVP-001              (that it works, and what does not)
            │
            ▼
   control   ─────────────────────▶  MF-CMP-001              (what is baselined and released)
```

Traceability runs in both directions: every requirement names the need it serves and is verified
by the entry with the same identifier; every design element names the requirements it realises;
every test suite names the requirements it covers; every deviation is recorded as an anomaly
rather than left implicit.

## Reading order

- **New to the project:** the [root README](../README.md), then
  [MF-SRS-001, clause 1](requirements.md#1-introduction) for what the product is, then
  [MF-SDD-001, clause 3](design.md#3-design-views) for how it is put together.
- **Changing behaviour:** the affected requirement in
  [MF-SRS-001, clause 3](requirements.md#3-specified-requirements), its verification entry in
  clause 4, the realising element in [MF-SDD-001, clause 5](design.md#5-traceability), and the
  change path in [MF-CMP-001, clause 5](configuration-management.md#5-configuration-change-control).
- **Reviewing or auditing:** [MF-VVP-001](verification-and-validation.md), in particular the open
  anomalies in clause 9.2 and the audits in
  [MF-CMP-001, clause 7](configuration-management.md#7-configuration-auditing).
- **Producing question-set files:** [MF-IFS-001](question-import-format.md) only; it is
  self-contained.

## Conventions

- **Normative keywords.** *Shall* is binding, *should* is a recommendation, *may* is a permission,
  *will* states a fact about the environment — as defined in
  [MF-SRS-001, 1.9](requirements.md#19-conventions).
- **Identifiers.** `REQ-<AREA>-<NN>` for requirements, `D-<nn>` for requirement decisions,
  `DD-<nn>` for design decisions, `A-<nn>` for anomalies, `TS-<AREA>` for test suites,
  `BL-<n>` for baselines. Identifiers are never reused.
- **One fact, one place.** Where a document needs a fact it does not own, it links to the owner.
  Duplicated facts drift apart; links do not.
- **Standards are cited, not reproduced.** The standards named above are copyrighted by their
  publishers; these documents cite clause numbers and follow their content provisions.
- **Tailoring is recorded.** Each document ends with a tailoring record stating what the standard
  asks for that this project does not do, and why. An unrecorded omission would make the
  conformance claim untrue.
