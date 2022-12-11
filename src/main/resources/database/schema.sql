// Help System
CREATE TABLE reserved_help_channels
(
	id          BIGINT PRIMARY KEY AUTO_INCREMENT,
	channel_id  BIGINT       NOT NULL UNIQUE,
	reserved_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	user_id     BIGINT       NOT NULL,
	timeout     INT          NOT NULL DEFAULT 60
);

CREATE TABLE help_channel_thanks
(
	id             BIGINT PRIMARY KEY AUTO_INCREMENT,
	reservation_id BIGINT       NOT NULL,
	user_id        BIGINT       NOT NULL,
	channel_id     BIGINT       NOT NULL,
	thanked_at     TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	helper_id      BIGINT       NOT NULL,
	CONSTRAINT help_channel_thanks_unique UNIQUE (reservation_id, helper_id)
);

CREATE TABLE help_account
(
	user_id    BIGINT PRIMARY KEY,
	experience DOUBLE NOT NULL
);

CREATE TABLE help_transaction
(
	id          BIGINT PRIMARY KEY AUTO_INCREMENT,
	recipient   BIGINT       NOT NULL,
	created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
	weight      DOUBLE       NOT NULL,
	messagetype INT          NOT NULL DEFAULT 0
);

// Question of the Week
CREATE TABLE qotw_question
(
	id              BIGINT PRIMARY KEY AUTO_INCREMENT,
	created_at      TIMESTAMP(0)  NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	guild_id        BIGINT        NOT NULL,
	created_by      BIGINT        NOT NULL,
	"TEXT"          VARCHAR(1024) NOT NULL,
	used            BOOLEAN       NOT NULL DEFAULT FALSE,
	question_number INTEGER       NULL     DEFAULT NULL,
	priority        INTEGER       NOT NULL DEFAULT 0
);

CREATE TABLE qotw_points
(
	user_id BIGINT PRIMARY KEY,
	points  BIGINT NOT NULL DEFAULT 0
);

// Warn
CREATE TABLE warn
(
	id              BIGINT PRIMARY KEY AUTO_INCREMENT,
	user_id         BIGINT        NOT NULL,
	warned_by       BIGINT        NOT NULL,
	created_at      TIMESTAMP(0)  NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
	severity        VARCHAR(32)   NOT NULL,
	severity_weight INT           NOT NULL,
	reason          VARCHAR(1024) NOT NULL,
	discarded       BOOL          NOT NULL DEFAULT FALSE
);

// Custom Tags
CREATE TABLE custom_tags
(
	id         BIGINT PRIMARY KEY AUTO_INCREMENT,
	guild_id   BIGINT        NOT NULL,
	created_by BIGINT        NOT NULL,
	name       VARCHAR(64)   NOT NULL,
	response   VARCHAR(2048) NOT NULL,
	reply      BOOL          NOT NULL DEFAULT TRUE,
	embed      BOOL          NOT NULL DEFAULT TRUE
);

// Starboard
CREATE TABLE starboard
(
	original_message_id  BIGINT PRIMARY KEY,
	guild_id             BIGINT NOT NULL,
	channel_id           BIGINT NOT NULL,
	author_id            BIGINT NOT NULL,
	starboard_message_id BIGINT NOT NULL
);

// Message Cache
CREATE TABLE message_cache
(
	message_id      BIGINT PRIMARY KEY,
	author_id       BIGINT        NOT NULL,
	message_content VARCHAR(4000) NOT NULL
);

// User Preferences
CREATE TABLE user_preferences
(
	user_id BIGINT  NOT NULL,
	ordinal INTEGER NOT NULL,
	state   VARCHAR NOT NULL DEFAULT '',
	PRIMARY KEY (user_id, ordinal)
)
