ALTER TABLE qotw_submissions
	ADD COLUMN status INTEGER NOT NULL DEFAULT 0;
UPDATE qotw_submissions
SET status = 0
WHERE NOT reviewed;
UPDATE qotw_submissions
SET status = 1
WHERE reviewed AND accepted;
UPDATE qotw_submissions
SET status = 2
WHERE reviewed AND NOT accepted;
ALTER TABLE qotw_submissions
	DROP COLUMN reviewed;
ALTER TABLE qotw_submissions
	DROP COLUMN accepted;
