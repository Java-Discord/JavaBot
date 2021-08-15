// Removes a Jam entity that was added by mistake.
DELETE FROM jam
WHERE completed = FALSE AND ends_at IS NOT NULL;
