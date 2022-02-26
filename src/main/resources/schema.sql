// Java Jam relations
CREATE TABLE jam_phase (
	name VARCHAR(64) PRIMARY KEY,
	description VARCHAR(1024) NOT NULL,
	next_phase VARCHAR(64) NULL DEFAULT NULL REFERENCES jam_phase(name)
		ON UPDATE CASCADE ON DELETE SET NULL
);
INSERT INTO jam_phase (name, description) VALUES
('Theme Planning', 'Experts will prepare a selection of themes for the Jam that you will be able to vote on.'),
('Theme Voting', 'Vote for the themes you would like to work on. The most popular theme(s) will be chosen for the Jam.'),
('Submission', 'Submit your project to the Jam during this phase.'),
('Submission Voting', 'Vote on the best submissions for the Jam.');
UPDATE jam_phase SET next_phase = 'Theme Voting' WHERE name = 'Theme Planning';
UPDATE jam_phase SET next_phase = 'Submission' WHERE name = 'Theme Voting';
UPDATE jam_phase SET next_phase = 'Submission Voting' WHERE name = 'Submission';

CREATE TABLE jam (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(64) NULL DEFAULT NULL,
	guild_id BIGINT NOT NULL,
	started_by BIGINT NOT NULL,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	starts_at DATE NOT NULL COMMENT 'Official start date of the jam. Usually the start of the month.',
	completed BOOLEAN NOT NULL DEFAULT FALSE,
	current_phase VARCHAR(64) NULL DEFAULT 'Theme Planning' REFERENCES jam_phase(name)
		ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE jam_theme (
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	jam_id BIGINT NOT NULL REFERENCES jam(id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	name VARCHAR(64) NOT NULL,
	PRIMARY KEY (jam_id, name),
	description VARCHAR(1024) NOT NULL DEFAULT '',
	accepted BOOLEAN NULL DEFAULT NULL
);

CREATE TABLE jam_theme_vote (
	user_id BIGINT NOT NULL,
	jam_id BIGINT NOT NULL,
	theme_name VARCHAR(64) NOT NULL,
	recorded_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	weight REAL NOT NULL DEFAULT 1.0,
	PRIMARY KEY (user_id, theme_name, jam_id),
	FOREIGN KEY (jam_id, theme_name) REFERENCES jam_theme(jam_id, name)
		ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE jam_submission (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	jam_id BIGINT NOT NULL REFERENCES jam(id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	theme_name VARCHAR(64) NOT NULL,
	user_id BIGINT NOT NULL,
	source_link VARCHAR(2048) NOT NULL COMMENT 'A link to the submission source.',
	description VARCHAR(2048) NOT NULL COMMENT 'The contents of the user submission message.',
	FOREIGN KEY (jam_id, theme_name) REFERENCES jam_theme(jam_id, name)
		ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE jam_submission_vote (
	user_id BIGINT NOT NULL,
	submission_id BIGINT NOT NULL REFERENCES jam_submission(id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	recorded_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	weight REAL NOT NULL DEFAULT 1.0,
	PRIMARY KEY (user_id, submission_id)
);

CREATE TABLE jam_message_id (
	jam_id BIGINT NOT NULL REFERENCES jam(id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	message_id BIGINT NOT NULL,
	PRIMARY KEY (jam_id, message_id),
	message_type VARCHAR(64) NOT NULL,
	UNIQUE (jam_id, message_type)
);

// Economy relations
CREATE TABLE economy_account (
	user_id BIGINT PRIMARY KEY,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	balance BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE economy_transaction (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	from_user_id BIGINT NULL,
	to_user_id BIGINT NULL,
	value BIGINT NOT NULL,
	message VARCHAR(127)
);

CREATE TABLE economy_account_preferences (
	user_id BIGINT PRIMARY KEY,
	receive_transaction_dms BOOLEAN NOT NULL DEFAULT TRUE
);

// Help system.
CREATE TABLE reserved_help_channels (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	channel_id BIGINT NOT NULL UNIQUE,
	reserved_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	user_id BIGINT NOT NULL,
	timeout INT NOT NULL DEFAULT 60
);

CREATE TABLE help_channel_thanks (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	reservation_id BIGINT NOT NULL,
	user_id BIGINT NOT NULL,
	channel_id BIGINT NOT NULL,
	thanked_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	helper_id BIGINT NOT NULL,
	CONSTRAINT help_channel_thanks_unique UNIQUE(reservation_id, helper_id)
);

// QOTW
CREATE TABLE qotw_question (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	guild_id BIGINT NOT NULL,
	created_by BIGINT NOT NULL,
	text VARCHAR(1024) NOT NULL,
	used BOOLEAN NOT NULL DEFAULT FALSE,
	question_number INTEGER NULL DEFAULT NULL,
	priority INTEGER NOT NULL DEFAULT 0
);

// Staff Management
CREATE TABLE staff_application (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	guild_id BIGINT NOT NULL,
	user_id BIGINT NOT NULL,
	name VARCHAR(127) NOT NULL,
	age INTEGER NOT NULL,
	email VARCHAR(254) NOT NULL,
	time_zone VARCHAR(63) NOT NULL,
	role_id BIGINT NOT NULL
);

// Warn
CREATE TABLE warn (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	warned_by BIGINT NOT NULL,
	created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	severity VARCHAR(32) NOT NULL,
	severity_weight INT NOT NULL,
	reason VARCHAR(1024) NOT NULL,
	discarded BOOL NOT NULL DEFAULT FALSE
);

// Custom Commands
CREATE TABLE custom_commands (
	id BIGINT PRIMARY KEY AUTO_INCREMENT,
	guild_id BIGINT NOT NULL,
	created_by BIGINT NOT NULL,
	name VARCHAR(64) NOT NULL,
	response VARCHAR(2048) NOT NULL,
	reply BOOL NOT NULL DEFAULT TRUE,
	embed BOOL NOT NULL DEFAULT TRUE
);

// QOTW Points
CREATE TABLE qotw_points (
	 user_id BIGINT PRIMARY KEY,
	 points BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE starboard (
	   original_message_id BIGINT PRIMARY KEY,
	   guild_id BIGINT NOT NULL,
	   channel_id BIGINT NOT NULL,
	   author_id BIGINT NOT NULL,
	   starboard_message_id BIGINT NOT NULL
);

CREATE TABLE qotw_submissions (
	  thread_id BIGINT PRIMARY KEY,
	  question_number INTEGER NOT NULL,
	  guild_id BIGINT NOT NULL,
	  author_id BIGINT NOT NULL,
	  reviewed BOOL NOT NULL DEFAULT FALSE,
	  accepted BOOL NOT NULL DEFAULT FALSE
);
