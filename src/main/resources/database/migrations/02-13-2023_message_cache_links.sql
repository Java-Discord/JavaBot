CREATE TABLE message_cache_attachments (
	message_id     		BIGINT NOT NULL,
	attachment_index	INT NOT NULL,
	link				VARCHAR(255),
	PRIMARY KEY(message_id, attachment_index)
)
