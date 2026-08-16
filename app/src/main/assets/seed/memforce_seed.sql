-- Demo data loaded once, when the database is first created.
-- Statements are split on a semicolon at the end of a line, so no literal below may contain one.
-- Users are inserted from DatabaseSeeder because their passwords must be hashed on the device.

INSERT INTO tags (_id, name) VALUES
    (1, 'advanced'),
    (2, 'algebra'),
    (3, 'basics'),
    (4, 'exam'),
    (5, 'formulas'),
    (6, 'humanities'),
    (7, 'science'),
    (8, 'space'),
    (9, 'trivia');

INSERT INTO categories (_id, name) VALUES
    (1, 'Astronomy'),
    (2, 'Biology'),
    (3, 'Chemistry'),
    (4, 'Geography'),
    (5, 'History'),
    (6, 'Mathematics'),
    (7, 'Programming'),
    (8, 'Sports');

INSERT INTO questions (_id, name, answer, category_id) VALUES
    (1, 'Which planet is closest to the Sun?', 'Mercury', 1),
    (2, 'What galaxy contains our Solar System?', 'The Milky Way', 1),
    (3, 'What force keeps planets in orbit?', 'Gravity', 1),
    (4, 'Which organelle produces energy in a cell?', 'The mitochondrion', 2),
    (5, 'What molecule carries genetic information?', 'DNA', 2),
    (6, 'How many chambers does the human heart have?', 'Four', 2),
    (7, 'What is the chemical symbol for gold?', 'Au', 3),
    (8, 'What is the pH of pure water at 25 degrees Celsius?', '7', 3),
    (9, 'Which gas do plants absorb during photosynthesis?', 'Carbon dioxide', 3),
    (10, 'What is the capital of Portugal?', 'Lisbon', 4),
    (11, 'Which river flows through Egypt?', 'The Nile', 4),
    (12, 'Which is the largest ocean on Earth?', 'The Pacific Ocean', 4),
    (13, 'In which year did the Second World War end?', '1945', 5),
    (14, 'Who was the first President of the United States?', 'George Washington', 5),
    (15, 'Which empire built the Colosseum?', 'The Roman Empire', 5),
    (16, 'What is the derivative of x squared?', '2x', 6),
    (17, 'How do you solve a quadratic equation?', 'With the quadratic formula', 6),
    (18, 'What is the value of pi to two decimal places?', '3.14', 6),
    (19, 'What does SQL stand for?', 'Structured Query Language', 7),
    (20, 'Which SQLite wildcard matches a single character?', 'The underscore character', 7),
    (21, 'What is the difference between a class and an object?', 'A class is a blueprint and an object is an instance of it', 7),
    (22, 'Which Android component draws a scrolling list?', 'RecyclerView', 7),
    (23, 'How many players are on a football pitch per team?', 'Eleven', 8),
    (24, 'How often are the Summer Olympics held?', 'Every four years', 8),
    (25, 'Which language is spoken in Brazil?', 'Portuguese', NULL);

INSERT INTO decks (_id, name, user_id) VALUES
    (1, 'Exam prep science', 1),
    (2, 'Quick trivia night', 1),
    (3, 'Weak spots', 1),
    (4, 'Algebra drill', 2),
    (5, 'Programming basics', 2);

INSERT INTO category_tags (category_id, tag_id) VALUES
    (1, 7), (1, 8),
    (2, 3), (2, 7),
    (3, 5), (3, 7),
    (4, 3), (4, 9),
    (5, 6), (5, 9),
    (6, 2), (6, 4), (6, 5),
    (7, 1), (7, 4),
    (8, 3), (8, 9);

INSERT INTO question_tags (question_id, tag_id) VALUES
    (1, 3), (1, 8),
    (2, 8), (2, 9),
    (3, 4), (3, 7), (3, 8),
    (4, 4), (4, 7),
    (5, 3), (5, 7),
    (6, 3), (6, 9),
    (7, 3), (7, 7),
    (8, 4), (8, 5), (8, 7),
    (9, 7), (9, 9),
    (10, 3), (10, 9),
    (11, 3), (11, 9),
    (12, 4), (12, 9),
    (13, 4), (13, 6),
    (14, 6), (14, 9),
    (15, 6), (15, 9),
    (16, 1), (16, 2), (16, 5),
    (17, 2), (17, 4), (17, 5),
    (18, 3), (18, 5),
    (19, 1), (19, 4),
    (20, 1), (20, 3),
    (21, 1), (21, 4),
    (22, 1), (22, 9),
    (23, 3), (23, 9),
    (24, 9),
    (25, 6), (25, 9);

INSERT INTO deck_questions (deck_id, question_id) VALUES
    (1, 3), (1, 4), (1, 8),
    (2, 2), (2, 10), (2, 14), (2, 23), (2, 24),
    (3, 16), (3, 17),
    (4, 16), (4, 17), (4, 18),
    (5, 19), (5, 20), (5, 21), (5, 22);
