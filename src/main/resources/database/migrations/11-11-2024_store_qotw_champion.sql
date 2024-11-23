CREATE TABLE qotw_champion (
	guild_id	BIGINT NOT NULL,
	user_id		BIGINT NOT NULL,
	PRIMARY KEY(guild_id, user_id)
)