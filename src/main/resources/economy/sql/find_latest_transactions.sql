SELECT *
FROM economy_transaction
WHERE from_user_id = ?
   OR to_user_id = ?
ORDER BY created_at DESC
/* LIMIT */;
