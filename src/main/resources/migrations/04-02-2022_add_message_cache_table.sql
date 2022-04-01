CREATE TABLE message_cache (
	message_id BIGINT PRIMARY KEY,
	author_id BIGINT NOT NULL,
	message_content VARCHAR(4000)
)