# MemForce demo script

A single pass through this script demonstrates every requirement in [requirements.md](requirements.md).
The demo data is loaded automatically the first time the database is created, so nothing has to be
typed in before the demo starts.

## Before you start

The seed only runs on a database that does not exist yet. Reset the app on the device first:

```
adb shell pm clear com.memforce
```

Then launch MemForce. The login screen appears.

### Demo accounts

| User | Password | Decks |
| --- | --- | --- |
| `ana` | `demo1234` | Exam prep science, Quick trivia night, Weak spots |
| `marko` | `demo1234` | Algebra drill, Programming basics |
| `petar` | `newpass123` | created live in phase 7 |

### Seeded data

* **9 tags** — advanced, algebra, basics, exam, formulas, humanities, science, space, trivia
* **8 categories** — Astronomy, Biology, Chemistry, Geography, History, Mathematics, Programming, Sports
* **25 questions** — spread across all categories, one deliberately left without a category
* **5 decks** — three owned by `ana`, two owned by `marko`

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

## Phase 3 — Categories

Tap **Categories**. Eight categories are listed, each with its tags underneath.

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 3.1 | Type `%y` | Astronomy, Biology, Chemistry, Geography, History | Search categories by name |
| 3.2 | Replace it with `_i%` | Biology, History — *i* in the second position | `_` in a fixed position |
| 3.3 | Replace it with `%o_y` | Astronomy, Biology, History | `_` between two `%` |
| 3.4 | Clear the search box, set *Filter by tag* to `science` | Astronomy, Biology, Chemistry | **Searching categories by tag** |
| 3.5 | Set *Filter by tag* back to `Any tag` | All eight categories return | — |
| 3.6 | Tap **Add**, enter `Music`, tap **Select tags**, tick `basics` and `humanities`, confirm, tap **Save** | `Music` appears with *basics, humanities* underneath | Adding a category with tags |
| 3.7 | Tap the `Sports` row, change the name to `Sports and games`, tap **Save** | The row is renamed and keeps its tags | Editing a category |
| 3.8 | Tap **Delete** on `Sports and games`, read the dialog, confirm | The dialog warns that 2 questions are affected. The category disappears; its questions survive without a category (checked in 4.5) | Deleting a category and handling the questions that used it |

Go back to the main menu.

## Phase 4 — Questions

Tap **Questions**. Twenty-five questions are listed, each showing *category | tags*.

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 4.1 | Type `Which%` | 9 questions | Search questions by name |
| 4.2 | Replace it with `How%` | 4 questions | `%` at the end |
| 4.3 | Replace it with `_ho%` | Only *Who was the first President of the United States?* | `_` in a fixed position |
| 4.4 | Replace it with `%capital%` | Only *What is the capital of Portugal?* | `%` on both sides |
| 4.5 | Clear the search box and scroll to the two former Sports questions (*How many players…*, *How often are the Summer Olympics held?*) | They are still listed but no longer show a category | Deleting a category edits the questions that used it |
| 4.6 | Set *Filter by category* to `Chemistry` | 3 questions | **Searching questions by category** |
| 4.7 | Keeping that filter, set *Filter by tag* to `exam` | Only *What is the pH of pure water at 25 degrees Celsius?* | **Searching questions by category and tag together** |
| 4.8 | Keeping both filters, type `%water%` | The same single question — name, category and tag all narrow the result at once | Combined search |
| 4.9 | Clear the search box and set both filters back to *Any* | All questions return | — |
| 4.10 | Tap **Add**, enter question `Which instrument has 88 keys?`, answer `The piano`, category `Music`, tags `basics` and `trivia`, tap **Save** | The new question appears under *Music \| basics, trivia* | Adding a question with a category and tags |
| 4.11 | Find *Which language is spoken in Brazil?* (it shows only tags, no category), tap it, set the category to `Geography`, tap **Save** | The row now shows *Geography \| humanities, trivia* | Editing a question |
| 4.12 | Set *Filter by category* to `Geography` | 4 questions, including the edited Brazil question | The edit really changed the relationship |
| 4.13 | Set the filter back to *Any category*, tap **Delete** on *How often are the Summer Olympics held?* and confirm | The question disappears from the list | Deleting a question |

Note before leaving: *Who was the first President of the United States?* currently shows
*History | humanities, trivia*.

Go back to the main menu.

## Phase 5 — Deleting a tag that is in use

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 5.1 | Tap **Tags**, tap **Delete** on `humanities`, read the dialog, confirm | The dialog explains the tag is removed from every question and category that uses it. `humanities` disappears | Deleting a tag |
| 5.2 | Go back, tap **Questions**, find *Who was the first President of the United States?* | It now shows *History \| trivia* — the tag was removed from the question | Deleting a tag edits the questions that used it |
| 5.3 | Open the *Filter by tag* spinner | `humanities` is no longer offered | The tag is gone everywhere |
| 5.4 | Go back, tap **Categories**, look at `History` | Its subtitle now shows only *trivia* | Deleting a tag edits the categories that used it |

Go back to the main menu.

## Phase 6 — Decks

Tap **My decks**. Three decks belonging to `ana` are listed.

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 6.1 | Read the question counts | Exam prep science 3, Quick trivia night **4**, Weak spots 2. The trivia deck dropped from 5 because the question deleted in 4.13 left it | Deleting a question keeps the decks consistent |
| 6.2 | Type `%night%` | Quick trivia night | **Searching decks** |
| 6.3 | Replace it with `_x%` | Exam prep science | `_` in a fixed position |
| 6.4 | Replace it with `%s` | Weak spots | `%` at the start |
| 6.5 | Clear the search box, tap **Add**, enter `Geography round`, tap **Select questions**, tick the three Geography questions, confirm, tap **Save** | `Geography round` appears with 3 questions | Adding a deck |
| 6.6 | Tap `Weak spots`, tap **Select questions**, additionally tick *What is the value of pi to two decimal places?*, confirm, tap **Save** | The deck now shows 3 questions instead of 2 | Editing a deck |
| 6.7 | Tap **Delete** on `Geography round` and confirm | The deck disappears; the dialog notes the questions themselves are kept | Deleting a deck |

Go back to the main menu.

## Phase 7 — Shared data versus individual decks

| # | Action | Expected result | Requirement |
| --- | --- | --- | --- |
| 7.1 | Tap **Sign out**, sign in as `marko` / `demo1234` | Main menu shows *Signed in as marko* | Login of a second existing user |
| 7.2 | Tap **My decks** | Only `Algebra drill` and `Programming basics`. None of ana's decks are visible | **Decks belong to one user only** |
| 7.3 | Go back, tap **Questions** and **Categories** | Ana's additions are here: the question *Which instrument has 88 keys?* and the category `Music`. Her deletions apply too | **Questions, categories and tags are shared by all users** |
| 7.4 | Go back, tap **Sign out**, sign in as `petar` / `newpass123` — a user that does not exist | Sign-in succeeds and the main menu shows *Signed in as petar* | **An unknown user is registered automatically** |
| 7.5 | Tap **My decks** | Empty — *Nothing matches this search*. The new user starts with no decks of his own | Decks are individual |
| 7.6 | Go back, tap **Sign out**, sign in as `petar` / `wrongpass` | *Wrong password for this user* | The credentials entered in 7.4 were written to the database |
| 7.7 | Sign in as `petar` / `newpass123` | Main menu opens again | The stored credentials work |

The demo is complete.

---

## Requirement coverage

| Requirement | Sample data used | Demonstrated in |
| --- | --- | --- |
| Login, password check, register unknown users (4 pts) | users `ana`, `marko`; `petar` created live | 1.1, 1.2, 7.1, 7.4, 7.6, 7.7 |
| Add a category, tag and question (2 pts) | tag `revision`, category `Music`, question *Which instrument has 88 keys?* | 2.5, 3.6, 4.10 |
| Edit a category, tag and question (4 pts) | `revision` → `revision 2026`, `Sports` → `Sports and games`, Brazil question gains `Geography` | 2.6, 3.7, 4.11 |
| Add and edit individual decks (4 pts) | `Geography round` added, `Weak spots` extended | 6.5, 6.6 |
| Delete categories and tags, fixing the questions that use them (2 pts) | `Sports and games` (2 questions), `humanities` (4 questions and 1 category) | 3.8, 4.5, 5.1, 5.2, 5.4 |
| Delete questions and decks (2 pts) | question *How often are the Summer Olympics held?*, deck `Geography round` | 4.13, 6.7 |
| Search categories, tags and questions by name (1 pt) | `%y` `_i%` `%o_y` / `a%` `%s` `%__a` `_x%` / `Which%` `How%` `_ho%` `%capital%` | 2.1–2.4, 3.1–3.3, 4.1–4.4 |
| Search questions by category and by tag (4 pts) | Chemistry × `exam` × `%water%` | 4.6, 4.7, 4.8 |
| Search individual decks (2 pts) | `%night%` `_x%` `%s` | 6.2, 6.3, 6.4 |
| Search categories by tag | `science` → Astronomy, Biology, Chemistry | 3.4 |
| Wildcards `%` and `_` supported everywhere | patterns listed above, on all four screens | 2.1–2.4, 3.1–3.3, 4.1–4.4, 6.2–6.4 |
| Questions, categories and tags are shared by all users | ana's `Music` category seen by marko | 7.3 |
| Decks are individual | ana's 3 decks versus marko's 2 versus petar's 0 | 7.2, 7.5 |
| Standalone app with SQLite on the device | `memforce.db`, seeded from `assets/seed/memforce_seed.sql` | Before you start |

## Changing the demo data

The data lives in [app/src/main/assets/seed/memforce_seed.sql](../app/src/main/assets/seed/memforce_seed.sql)
and is executed by `DatabaseSeeder` when `MemForceDbHelper` creates the database. Statements are
split on a semicolon at the end of a line, so no text in that file may contain one. Re-run
`adb shell pm clear com.memforce` after any change to load it again.
