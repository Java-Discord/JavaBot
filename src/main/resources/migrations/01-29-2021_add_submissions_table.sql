CREATE TABLE qotw_submissions (
  thread_id BIGINT PRIMARY KEY,
  question_number INTEGER NOT NULL,
  guild_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  reviewed BOOL NOT NULL DEFAULT FALSE,
  accepted BOOL NOT NULL DEFAULT FALSE
);