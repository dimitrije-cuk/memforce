# Question import format

| Field | Value |
|---|---|
| Document title | MemForce — Question import format |
| Document identifier | MF-IFS-001 |
| Version | 1.0 (defines `formatVersion` `"1.0"`) |
| Date | 2026-09-10 |
| Status | Draft — proposed for baseline **BL-1**, which is declared when the tag is applied ([MF-CMP-001, 4.3](configuration-management.md#43-baselines)) |
| Controlled by | [MF-CMP-001, clause 8.1](configuration-management.md#81-question-set-format) — a change to this format is a change to other people's files |
| Implements | [REQ-IMP-10 … REQ-IMP-100](requirements.md#325-question-set-import), [REQ-STD-30](requirements.md#37-standards-compliance) |

MemForce describes a batch of quiz questions as a single JSON
**question set** file. The intent is that a user copies the [template](templates/question-set-template.json)
into an LLM chatbot, asks it to fill in questions on a topic, and then imports the JSON file it
returns into MemForce. This document defines that file format so that:

* the LLM has an unambiguous structure to fill in,
* MemForce has a stable, validated shape to import, and
* both can evolve independently as long as `formatVersion` is respected.

This document covers the **format and validation rules**; what the app does with a valid file is
described under [Importing a question set](#importing-a-question-set).

## Importing a question set

**Questions → Import** opens the system file picker, which offers files of type
`application/json`, `text/plain` and `application/octet-stream` — the three types under which a
chatbot's saved `.json` file usually arrives. MemForce reads the chosen file once, for reading
only, and never writes to it. Once the file validates, MemForce shows how many questions and tags
it would add and imports it only after that is confirmed. The import follows these rules:

* A file is taken **as a whole or not at all**: if it breaks any of the
  [validation rules](#validation-rules), the problems are listed and nothing is written. The
  writing itself happens in one database transaction, so an interruption partway through leaves
  the database exactly as it was.
* **The file must be no larger than 1 MiB.** It is read into memory as a whole, and a bound is
  what keeps an unexpected file from exhausting memory on a small device. 1 MiB holds several
  thousand questions.
* **Tags are created on demand.** A tag name that no existing tag matches (case-insensitively)
  becomes a new tag, exactly as adding one by hand would.
* **A question whose text already exists is not stored twice.** Matching is case-insensitive; the
  existing question gains the file's tags instead. Its stored answer is kept, and an answer from
  the file is only used when the existing question has none.
* Entries repeating the same question text **within one file** are collapsed into a single
  question carrying the tags of all of them.
* Questions and tags are shared by every user, so an import is visible to everyone.

## Files

| File | Purpose |
| --- | --- |
| [question-set.schema.json](schemas/question-set.schema.json) | Machine-readable [JSON Schema](https://json-schema.org/) (draft-07) that a question set must validate against. Authoritative source for field types, limits, and required fields. |
| [question-set-template.json](templates/question-set-template.json) | Blank template with placeholder text. Copy this into an LLM chatbot together with the prompt below. |
| [question-set-example.json](templates/question-set-example.json) | A filled-in example (the history set used throughout this document) for reference. |

## Structure

A question set is a single JSON object:

```json
{
  "formatVersion": "1.0",
  "name": "World history basics",
  "description": "A short mixed-era history set.",
  "tags": ["history", "world-history"],
  "questions": [
    {
      "question": "Who was the first emperor of Rome?",
      "answer": "Augustus",
      "tags": ["ancient-history", "1st-century-bc"]
    },
    {
      "question": "In which year did the French Revolution begin?",
      "answer": "1789",
      "tags": ["18th-century", "france"]
    }
  ]
}
```

### Top-level fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `formatVersion` | string | yes | Format version. Currently only `"1.0"` is defined; a future incompatible change to this document bumps this value so importers can tell old files from new ones. |
| `name` | string | no | Short label for the set, e.g. for display in a file picker. Not stored in the database. |
| `description` | string | no | Free-text summary of what the set covers. Not stored in the database. |
| `tags` | string[] | no (defaults to `[]`) | **Set-level tags**, inherited by every question in `questions` (see [Tag inheritance](#tag-inheritance)). |
| `questions` | object[] | yes, at least one entry | The questions to import. |

### Question object fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `question` | string | yes | The question text. Maps to a question's name. |
| `answer` | string or `null` | no | The answer text. MemForce allows questions with no stored answer, so this may be omitted or `null`. |
| `tags` | string[] | no (defaults to `[]`) | **Question-level tags**, additional to the set-level tags, that apply only to this question. |

## Tag inheritance

Tags exist at two levels so that a set covering a broad subject can still distinguish questions
within it:

* **Set-level `tags`** apply to *every* question in the file. Use this for the tag(s) that
  describe the whole set, e.g. `"history"`.
* **Question-level `tags`** apply only to that one question, on top of the set-level tags. Use
  this for anything that varies from question to question, e.g. `"ancient-history"`,
  `"18th-century"`, `"france"`.

The tags actually assigned to an imported question are the **union** of the set-level tags and
that question's own tags, deduplicated case-insensitively (MemForce's `tags` table is unique
case-insensitively — see [DbContract.Tags](../app/src/main/java/com/memforce/db/DbContract.java)).
For example, the first question in the [structure](#structure) example above ends up tagged
`history`, `world-history`, `ancient-history`, `1st-century-bc`.

Either level may be empty, but a question set should not rely on both being empty for every
question — at least one tag (set-level or question-level) should apply to each question so it
remains discoverable by MemForce's tag search.

## Validation rules

A question set is only valid if it matches [question-set.schema.json](schemas/question-set.schema.json).
In summary:

1. `formatVersion` must be present and equal to a supported version (currently `"1.0"`).
2. `questions` must be present and contain at least one entry.
3. Every question must have a non-empty `question` string (max 1000 characters).
4. `answer`, when present, is a string or `null` (max 2000 characters).
5. Tag names (set-level or question-level) are non-empty strings of a single line (max 50
   characters) that neither start nor end with whitespace. Lower-case kebab-case (e.g.
   `world-history`) is recommended so visually distinct tags don't collide once matched
   case-insensitively.
6. `tags` arrays must not contain duplicate entries *within the same array* (`uniqueItems`); the
   set-level and a question's own tags may legitimately repeat each other, since that is exactly
   how inheritance is expressed — those duplicates are removed at merge time, per
   [Tag inheritance](#tag-inheritance).
7. No unrecognized top-level or per-question fields are allowed (`additionalProperties: false`),
   so an LLM cannot silently invent extra structure the importer doesn't understand.
8. The document must be a **single JSON object**, no larger than **1 MiB**, encoded in UTF-8.

Surrounding whitespace is trimmed when a file is read, and a value left blank by that trimming
counts as absent: a `question` of only spaces is rejected as empty, while a blank `answer` or
`description` simply means the field was not given. An *empty* `name` breaks the schema's
`minLength` and is rejected, but a `name` of only spaces is treated as absent. A file may start
with a UTF-8 byte order mark; it is ignored. Nothing but whitespace may follow the closing brace,
so an LLM that adds a sentence after the JSON is caught rather than silently half-imported.

### Two rules the importer applies beyond the schema

The JSON Schema is the authoritative statement of the format, but a schema cannot express
everything the importer needs, so two checks go further. A file that a generic JSON Schema
validator accepts may still be rejected by MemForce for these reasons — and only these:

* **Tag names must stay on one line.** The schema's pattern forbids leading and trailing
  whitespace; the importer additionally rejects a tag containing a line feed, a carriage return,
  or the Unicode line and paragraph separators. A tag is a label, and a label that wraps is
  almost always a chatbot's stray newline.
* **Whitespace is judged more broadly.** The importer treats non-breaking spaces and other
  Unicode space characters as whitespace, so a tag padded with them is rejected as blank rather
  than stored as a look-alike of an existing tag.

Note also that `uniqueItems` compares tag entries exactly, so `["Math", "math"]` passes
validation — and then merges into a single tag, because MemForce matches tag names
case-insensitively.

Two further differences are worth knowing when a file is generated or validated by other tools:

* **Length limits are counted in UTF-16 units, not code points.** JSON Schema counts `maxLength`
  in characters (code points), while the importer counts Java string length. They agree for all
  ordinary text; they differ for characters outside the Basic Multilingual Plane, such as emoji,
  which count as two. A tag of 26 emoji satisfies the schema and is rejected by the importer.
* **Bytes that are not valid UTF-8 are replaced, not rejected.** The file is decoded leniently, so
  a malformed byte sequence becomes the replacement character `U+FFFD` and travels into the stored
  text rather than failing the import.

### How violations are reported

Validation does not stop at the first problem: the whole file is checked, every violation is
collected, and they are reported together, so a file can be fixed in one pass instead of one
error per attempt. The dialog lists up to ten violations followed by a count of the remainder.
Nothing is written in any case.

What the importer does with a valid file — merging into an existing question, creating tags on
demand — is described under [Importing a question set](#importing-a-question-set).

## Prompt to use with an LLM chatbot

Paste the contents of [question-set-template.json](templates/question-set-template.json) into a
chatbot along with instructions similar to:

> Fill in this JSON template with 10 quiz questions about the French Revolution. Keep
> `formatVersion` unchanged, set `tags` to the subject-level tags for the whole set, and give each
> question its own additional `tags` where relevant (e.g. a decade or sub-topic). Keep every
> field name and the overall structure exactly as given. Return only the JSON.

Save the result as a `.json` file and import it with **Questions → Import**.

## Extensibility

`formatVersion` exists specifically so this format can change later without breaking older files:
an importer can inspect `formatVersion` and either upgrade the file, reject it with a clear
message, or apply version-specific parsing. Fields not yet needed (for example per-question
difficulty, a source/citation field, or media attachments) can be added in a future minor version
as optional fields without breaking `1.0` files, or as a `2.0` version if they change existing
fields' meaning.

Which of those two a given change requires is not a matter of judgement: the rule is stated in
[MF-CMP-001, clause 8.1](configuration-management.md#81-question-set-format), and this document,
the JSON Schema, the template, the example and the parser all change in the same commit.
