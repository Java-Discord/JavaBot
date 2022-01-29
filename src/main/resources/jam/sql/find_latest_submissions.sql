SELECT js.*
FROM jam_submission js
WHERE js.jam_id = ? AND js.id = (
    SELECT js2.id
    FROM jam_submission js2
    WHERE js2.jam_id = js.jam_id AND js2.user_id = js.user_id AND js2.theme_name = js.theme_name
    ORDER BY js2.id DESC
    LIMIT 1
)
ORDER BY js.created_at;
