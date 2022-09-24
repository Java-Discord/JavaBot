CREATE TABLE user_preferences
(
	user_id BIGINT  NOT NULL,
	ordinal INTEGER NOT NULL,
	state   VARCHAR NOT NULL DEFAULT '',
	PRIMARY KEY (user_id, ordinal)
)