SELECT *
FROM jam_theme
WHERE accepted = TRUE AND jam_id = ?
LIMIT 1;
