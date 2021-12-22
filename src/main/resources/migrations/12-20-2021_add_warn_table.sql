CREATE TABLE warn (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL,
      warned_by BIGINT NOT NULL,
      created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
      severity VARCHAR(32) NOT NULL,
      severity_weight INT NOT NULL,
      reason VARCHAR(1024) NOT NULL,
      discarded BOOL NOT NULL DEFAULT FALSE
);