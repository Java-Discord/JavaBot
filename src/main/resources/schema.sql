CREATE TABLE jam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    starts_at DATE NOT NULL COMMENT 'Official start date of the jam. Usually the start of the month.',
    theme_planning_starts_at TIMESTAMP(0) NOT NULL COMMENT 'Start of the theme planning period.',
    theme_planning_ends_at TIMESTAMP(0) NOT NULL COMMENT 'End of the theme planning period.',
    theme_voting_starts_at TIMESTAMP(0) NOT NULL COMMENT 'Start of the theme voting period.',
    theme_voting_ends_at TIMESTAMP(0) NOT NULL COMMENT 'End of the theme voting period.',
    submission_starts_at TIMESTAMP(0) NOT NULL COMMENT 'Start of the submission period.',
    submission_ends_at TIMESTAMP(0) NOT NULL COMMENT 'End of the submission period.',
    submission_voting_starts_at TIMESTAMP(0) NOT NULL COMMENT 'Start of the submission voting period.',
    submission_voting_ends_at TIMESTAMP(0) NOT NULL COMMENT 'End of the submission voting period.',
    CHECK (
        // Ensure that all periods have a logical start and end.
        DATEDIFF('SECOND', theme_planning_starts_at, theme_planning_ends_at) > 0 AND
        DATEDIFF('SECOND', theme_voting_starts_at, theme_voting_ends_at) > 0 AND
        DATEDIFF('SECOND', submission_starts_at, submission_ends_at) > 0 AND
        DATEDIFF('SECOND', submission_voting_starts_at, submission_voting_ends_at) > 0 AND
        // Ensure that periods don't overlap.
        DATEDIFF('SECOND', theme_planning_ends_at, theme_voting_starts_at) >= 0 AND
        DATEDIFF('SECOND', theme_voting_ends_at, submission_starts_at) >= 0 AND
        DATEDIFF('SECOND', submission_ends_at, submission_voting_starts_at) >= 0
    )
);

CREATE TABLE jam_theme (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    jam_id BIGINT NOT NULL REFERENCES jam(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(1024) NOT NULL DEFAULT '',
    accepted BOOLEAN NULL DEFAULT NULL,
    UNIQUE (jam_id, name)
);

CREATE TABLE jam_theme_vote (
    user_id BIGINT NOT NULL,
    theme_id BIGINT NOT NULL REFERENCES jam_theme(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    recorded_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    weight REAL NOT NULL DEFAULT 1.0,
    PRIMARY KEY (user_id, theme_id)
);

CREATE TABLE jam_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    jam_id BIGINT NOT NULL REFERENCES jam(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    theme_id BIGINT NULL DEFAULT NULL REFERENCES jam_theme(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    submission_content VARCHAR(2048) NOT NULL,
    UNIQUE (jam_id, theme_id, user_id)
);

CREATE TABLE jam_submission_vote (
    user_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL REFERENCES jam_submission(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    recorded_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    weight REAL NOT NULL DEFAULT 1.0,
    PRIMARY KEY (user_id, submission_id)
);
