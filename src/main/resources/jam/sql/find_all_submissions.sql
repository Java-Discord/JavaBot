SELECT js.*
FROM jam_submission js
WHERE js.jam_id = ? /* CONDITIONS */
ORDER BY js.created_at
LIMIT 10
/* OFFSET */;
