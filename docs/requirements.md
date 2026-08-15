# MemForce

**Second Android Assignment (Academic Year 2025/2026)**

## Assignment Objective

The goal of this assignment is to implement an application for entering and searching quiz questions, as well as creating decks based on arbitrary user selections of questions. The application must also implement a user login system. However, all functionality will be implemented on a single device, meaning there is no client-server architecture. Nevertheless, a database must be used to implement all required functionality.

## Assignment Description

* The application should be developed as a **standalone application** (it should not require a connection to a server to function).

* The **SQLite database** should be stored on the phone (or on the emulator, if you are using one).

* The following functionality must be provided:

  * **User login** for entering the application. If the entered user does not already exist in the database table, the user should be added to the table with the entered username and password.

  * **Adding, deleting, and editing questions**

  * **Adding, deleting, and editing categories**

  * **Adding, deleting, and editing tags**

  * **Adding, deleting, and editing decks**

  * **Searching for questions by category**

  * **Searching for questions by tag**

  * **Searching for categories by tag**

* You may implement the above functionality however you see fit. One suggested approach is to use one activity (or fragment) for questions, one for categories, one for tags, and one for decks. Each activity/fragment should provide the ability to add new data, search existing data according to some criterion, delete items, and edit items.

## Notes

### 1. SQLite Wildcard Characters

SQLite has two types of wildcard characters:

* `%` represents a sequence of **zero or more characters**.
* `_` represents **exactly one character**.

### 2. Using Wildcards for More Complex Searches

The wildcard characters above can be used to perform more complex searches, such as:

* `po%` — any word that starts with the letters **"po"**
* `%ta` — any word that ends with **"ta"**
* `%sto%` — any word containing the letters **"sto"** at any position
* `_br%` — any word that has **"b" in the second position and "r" in the third position**
* `%__a` — a word with **at least three letters** that ends with **"a"**

These wildcard searches should be supported when searching questions, categories, and tags.

### 3. Shared vs. Individual Data

Keep in mind that **questions, categories, and tags** that are added, deleted, or edited must be visible to **all users**.

Decks, however, are **individual**. If a user creates, edits, or displays decks, those decks should only belong to and be visible to that user. Other users should create, edit, and display **their own decks**.

### 4. Required Database Tables

The following tables are required:

* **Users**
* **Categories**
* **Questions**
* **Tags**
* **Decks**

The required fields for these tables are:

**a. Users**

* `id`
* `name`
* `password`

**b. Categories**

* `id`
* `name`
* `tag`

**c. Tags**

* `id`
* `name`

**d. Questions**

* `id`
* `name`
* `tag`
* `category`

**e. Decks**

* `id`
* `name`

### 5. Database Design

The initial database structure from point 4 (the tables and their fields) will need to be **extended and modified** in order to implement all of the required functionality in the application.

In particular, the database design should be extended as necessary to support:

* relationships between questions and categories,
* relationships between questions and tags,
* relationships between decks and questions,
* ownership of decks by individual users,
* searching and filtering according to the required criteria.

# Grading

* **Logging into the system**, checking the username and password, and updating login information in the database for newly added users — **4 points**

* Ability to **add a new category, tag, and question** — **2 points**

* Ability to **edit an existing category, tag, and question** — **4 points**

* Ability to **add and edit individual decks** — **4 points**

* Ability to **delete categories and tags** — **2 points**

  * Keep in mind that deleting a category or tag requires you to **edit or delete the questions in which they appear**.

* Ability to **delete questions and decks** — **2 points**

* Ability to **search for categories, tags, and questions by name** — **1 point**

* Ability to **search for questions by category and tag** — **4 points**

* Ability to **search individual decks** — **2 points**