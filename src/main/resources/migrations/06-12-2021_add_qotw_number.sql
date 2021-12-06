ALTER TABLE qotw_question
    ADD COLUMN question_number INTEGER NULL DEFAULT NULL AFTER used;
