UPDATE qotw_submissions
SET status = 0 WHERE NOT reviewed;

UPDATE qotw_submissions
SET status = 1 WHERE reviewed AND accepted;

UPDATE qotw_submissions
SET status = 2 WHERE reviewed AND NOT accepted;
