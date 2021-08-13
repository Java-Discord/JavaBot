// Adds tables for managing the QOTW questions.

CREATE TABLE qotw_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    guild_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    text VARCHAR(1024) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Set to TRUE when this question has been used.',
    priority INTEGER NOT NULL DEFAULT 0
);


