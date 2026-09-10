# MemForce demo script

A single pass through this script demonstrates every feature the app implements.

> Decks are not implemented. The deck requirements listed in [requirements.md](requirements.md)
> (§4 *Decks*, and the deck items under *Grading*) are intentionally out of scope, so this script
> does not cover them.

The demo data is loaded automatically the first time the database is created, so nothing has to be
typed in before the demo starts.

## Before you start

The seed only runs on a database that does not exist yet. Reset the app on the device first:

```
adb shell pm clear com.memforce
```

Then launch MemForce. The login screen appears.

### Demo accounts

| User | Password | Notes |
| --- | --- | --- |
| `ana` | `demo1234` | seeded |
| `marko` | `demo1234` | seeded |
| `petar` | `newpass123` | created live in phase 5 |

### Seeded data

* **9 tags** — advanced, algebra, basics, exam, formulas, humanities, science, space, trivia
* **25 questions** — a general-knowledge mix, each carrying one or more tags

### About the search boxes

Search runs the typed text straight through SQL `LIKE`, so wildcards keep their meaning and a
plain word only matches a whole name. Type the patterns exactly as written below:
`%` stands for any run of characters, `_` for exactly one character.

---

## Phase 1 — Login

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 1.1 | Enter user `ana`, password `wrongpass`, tap **Sign in** | Error under the password field: *Wrong password for this user* | Login checks the password against the database |
| 1.2 | Correct the password to `demo1234`, tap **Sign in** | Main menu opens showing *Signed in as ana* | Login of an existing user |

## Phase 2 — Tags

Tap **Tags** on the main menu. Nine tags are listed.

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 2.1 | Type `a%` in *Search by name* | advanced, algebra | Search tags by name, `%` at the end |
| 2.2 | Replace it with `%s` | basics, formulas, humanities | `%` at the start |
| 2.3 | Replace it with `%__a` | algebra, trivia — names of at least three letters ending in *a* | `_` combined with `%` |
| 2.4 | Replace it with `_x%` | exam — *x* in the second position | `_` in a fixed position |
| 2.5 | Clear the search box, tap **Add**, enter `revision`, tap **Save** | `revision` appears in the list | Adding a tag |
| 2.6 | Tap the `revision` row, change the name to `revision 2026`, tap **Save** | The row now reads `revision 2026` | Editing a tag |

Go back to the main menu.

## Phase 3 — Questions

Tap **Questions**. Twenty-five questions are listed, each showing its tags.

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 3.1 | Type `Which%` | 9 questions | Search questions by name |
| 3.2 | Replace it with `How%` | 4 questions | `%` at the end |
| 3.3 | Replace it with `_ho%` | Only *Who was the first President of the United States?* | `_` in a fixed position |
| 3.4 | Replace it with `%capital%` | Only *What is the capital of Portugal?* | `%` on both sides |
| 3.5 | Clear the search box, set *Filter by tag* to `science` | 6 questions | **Searching questions by tag** |
| 3.6 | Keeping that filter, type `%water%` | Only *What is the pH of pure water at 25 degrees Celsius?* — name and tag narrow the result at once | Combined search |
| 3.7 | Clear the search box and set the filter back to *Any tag* | All questions return | — |
| 3.8 | Tap **Add**, enter question `Which instrument has 88 keys?`, answer `The piano`, tags `basics` and `trivia`, tap **Save** | The new question appears under *basics, trivia* | Adding a question with tags |
| 3.9 | Find *Which language is spoken in Brazil?*, tap it, tap **Select tags**, additionally tick `basics`, confirm, tap **Save** | The row now shows *basics, humanities, trivia* | Editing a question |
| 3.10 | Set *Filter by tag* to `basics` | 11 questions, including the edited Brazil question | The edit really changed the relationship |
| 3.11 | Set the filter back to *Any tag*, tap **Delete** on *How often are the Summer Olympics held?* and confirm | The question disappears from the list | Deleting a question |

Note before leaving: *Who was the first President of the United States?* currently shows
*humanities, trivia*.

Go back to the main menu.

## Phase 4 — Deleting a tag that is in use

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 4.1 | Tap **Tags**, tap **Delete** on `humanities`, read the dialog, confirm | The dialog explains the tag is removed from every question that uses it. `humanities` disappears | Deleting a tag |
| 4.2 | Go back, tap **Questions**, find *Who was the first President of the United States?* | It now shows only *trivia* — the tag was removed from the question | Deleting a tag edits the questions that used it |
| 4.3 | Open the *Filter by tag* spinner | `humanities` is no longer offered | The tag is gone everywhere |

Go back to the main menu.

## Phase 5 — Shared data across users

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 5.1 | Tap **Sign out**, sign in as `marko` / `demo1234` | Main menu shows *Signed in as marko* | Login of a second existing user |
| 5.2 | Tap **Questions** and **Tags** | Ana's additions are here: the question *Which instrument has 88 keys?* and the tag `revision 2026`. Her deletions apply too | **Questions and tags are shared by all users** |
| 5.3 | Go back, tap **Sign out**, sign in as `petar` / `newpass123` — a user that does not exist | Sign-in succeeds and the main menu shows *Signed in as petar* | **An unknown user is registered automatically** |
| 5.4 | Tap **Sign out**, sign in as `petar` / `wrongpass` | *Wrong password for this user* | The credentials entered in 5.3 were written to the database |
| 5.5 | Sign in as `petar` / `newpass123` | Main menu opens again | The stored credentials work |

The demo is complete.

---

## Requirement coverage

| Requirement | Sample data used | Demonstrated in |
| --- | --- | --- |
| Login, password check, register unknown users (4 pts) | users `ana`, `marko`; `petar` created live | 1.1, 1.2, 5.1, 5.3, 5.4, 5.5 |
| Add a tag and question (2 pts) | tag `revision`, question *Which instrument has 88 keys?* | 2.5, 3.8 |
| Edit a tag and question (4 pts) | `revision` → `revision 2026`, Brazil question gains `basics` | 2.6, 3.9 |
| Delete tags, fixing the questions that use them (2 pts) | `humanities` (4 questions) | 4.1, 4.2 |
| Delete questions (2 pts) | question *How often are the Summer Olympics held?* | 3.11 |
| Search tags and questions by name (1 pt) | `a%` `%s` `%__a` `_x%` / `Which%` `How%` `_ho%` `%capital%` | 2.1–2.4, 3.1–3.4 |
| Search questions by tag (4 pts) | `science`, narrowed with `%water%` | 3.5, 3.6 |
| Wildcards `%` and `_` supported everywhere | patterns listed above, on both screens | 2.1–2.4, 3.1–3.4 |
| Questions and tags are shared by all users | ana's question *Which instrument has 88 keys?* seen by marko | 5.2 |
| Standalone app with SQLite on the device | `memforce.db`, seeded from `assets/seed/memforce_seed.sql` | Before you start |

## Changing the demo data

The data lives in [app/src/main/assets/seed/memforce_seed.sql](../app/src/main/assets/seed/memforce_seed.sql)
and is executed by `DatabaseSeeder` when `MemForceDbHelper` creates the database. Statements are
split on a semicolon at the end of a line, so no text in that file may contain one. Re-run
`adb shell pm clear com.memforce` after any change to load it again.
