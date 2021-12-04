ALTER TABLE reserved_help_channels
    DROP CONSTRAINT CONSTRAINT_CC2;
ALTER TABLE reserved_help_channels
    ADD COLUMN id BIGINT PRIMARY KEY AUTO_INCREMENT BEFORE channel_id;
ALTER TABLE reserved_help_channels
    ADD CONSTRAINT reserved_help_channels_channel_unique UNIQUE(channel_id);
