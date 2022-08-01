CREATE TABLE user_preferences
(
	user_id BIGINT  NOT NULL,
	ordinal INTEGER NOT NULL,
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	PRIMARY KEY (user_id, ordinal)
)