CREATE TABLE staff_activity_messages (
	guild_id	BIGINT NOT NULL,
	user_id		BIGINT NOT NULL,
	message_id	BIGINT NOT NULL,
	PRIMARY KEY(guild_id, user_id)
)