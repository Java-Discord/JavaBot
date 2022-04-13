CREATE TABLE help_account (
  user_id BIGINT PRIMARY KEY,
  experience DOUBLE NOT NULL,
  help_contributions INT NOT NULL DEFAULT 0
);

CREATE TABLE help_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipient BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  value DOUBLE NOT NULL,
  message VARCHAR(255)
);