// Adds a table that holds a list of preferences for each economy account.
CREATE TABLE economy_account_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES economy_account(user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    receive_transaction_dms BOOLEAN NOT NULL DEFAULT TRUE
);

// Populate the table for each new account.
INSERT INTO economy_account_preferences (user_id)
SELECT user_id
FROM economy_account;
