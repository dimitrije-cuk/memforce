# Question import format

MemForce can (as of this specification) describe a batch of quiz questions as a single JSON
**question set** file. The intent is that a user copies the [template](templates/question-set-template.json)
into an LLM chatbot, asks it to fill in questions on a topic, and then imports the JSON file it
returns into MemForce. This document defines that file format so that:

* the LLM has an unambiguous structure to fill in,
* MemForce has a stable, validated shape to import, and
* both can evolve independently as long as `formatVersion` is respected.

This document covers the **format and validation rules only**. The in-app import workflow
(where the "Import" action lives, how conflicts are resolved in the UI, etc.) is implemented in a
later phase; see [Status](#status) below.

## Status

* **Phase 1 (this change):** format definition, JSON Schema, template, and documentation.
* **Phase 2 (future work):** an in-app "Import question set" action that reads a file matching
  this format and writes the resulting tags and questions to the database.

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
case-insensitively — see [DbContract.Tags](/app/src/main/java/com/memforce/db/DbContract.java)).
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
5. Tag names (set-level or question-level) are non-empty, non-blank strings (max 50 characters).
   Lower-case kebab-case (e.g. `world-history`) is recommended so visually distinct tags don't
   collide once matched case-insensitively.
6. `tags` arrays must not contain duplicate entries *within the same array* (`uniqueItems`); the
   set-level and a question's own tags may legitimately repeat each other, since that is exactly
   how inheritance is expressed — those duplicates are removed at merge time, per
   [Tag inheritance](#tag-inheritance).
7. No unrecognized top-level or per-question fields are allowed (`additionalProperties: false`),
   so an LLM cannot silently invent extra structure the importer doesn't understand.

Future importer behavior (to be finalized when Phase 2 is implemented) should also define, and
this document should be updated to record:

* whether importing a question whose text already exists (case-insensitively) merges tags into
  the existing question or creates a duplicate, and
* whether tag names are created on demand (matching the existing "add a new tag" behavior) when
  they don't already exist.

## Prompt to use with an LLM chatbot

Paste the contents of [question-set-template.json](templates/question-set-template.json) into a
chatbot along with instructions similar to:

> Fill in this JSON template with 10 quiz questions about the French Revolution. Keep
> `formatVersion` unchanged, set `tags` to the subject-level tags for the whole set, and give each
> question its own additional `tags` where relevant (e.g. a decade or sub-topic). Keep every
> field name and the overall structure exactly as given. Return only the JSON.

Save the result as a `.json` file; it can then be imported into MemForce once the import action
described in [Status](#status) ships.

## Extensibility

`formatVersion` exists specifically so this format can change later without breaking older files:
an importer can inspect `formatVersion` and either upgrade the file, reject it with a clear
message, or apply version-specific parsing. Fields not yet needed (for example per-question
difficulty, a source/citation field, or media attachments) can be added in a future minor version
as optional fields without breaking `1.0` files, or as a `2.0` version if they change existing
fields' meaning.
