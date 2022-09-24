TRUNCATE TABLE user_preferences;

ALTER TABLE user_preferences DROP COLUMN enabled;

ALTER TABLE user_preferences ADD COLUMN state VARCHAR NOT NULL DEFAULT ''