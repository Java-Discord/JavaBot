// Delete all data.
DELETE FROM help_channel_thanks WHERE TRUE;
ALTER TABLE help_channel_thanks
    ADD COLUMN reservation_id BIGINT NOT NULL AFTER id;
ALTER TABLE help_channel_thanks
    ADD CONSTRAINT help_channel_thanks_unique UNIQUE(reservation_id, helper_id);
