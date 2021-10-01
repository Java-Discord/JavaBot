CREATE TABLE reserved_help_channels (
    channel_id BIGINT PRIMARY KEY,
    reserved_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    user_id BIGINT NOT NULL
);
