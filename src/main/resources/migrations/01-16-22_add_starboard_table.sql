CREATE TABLE starboard (
 original_message_id BIGINT PRIMARY KEY,
 guild_id BIGINT NOT NULL,
 channel_id BIGINT NOT NULL,
 author_id BIGINT NOT NULL,
 starboard_message_id BIGINT NOT NULL
);