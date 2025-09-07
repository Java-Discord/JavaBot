CREATE TABLE forms (
    form_id BIGINT NOT NULL,
    form_data VARCHAR NOT NULL,
    title VARCHAR NOT NULL,
    submit_message VARCHAR,
    submit_channel VARCHAR NOT NULL,
    message_id VARCHAR,
    message_channel VARCHAR,
    expiration BIGINT NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    onetime BOOLEAN NOT NULL,
    PRIMARY KEY (form_id)
);

CREATE TABLE form_submissions (
    "timestamp" BIGINT NOT NULL,
    user_id VARCHAR NOT NULL,
    form_id BIGINT NOT NULL,
    user_name VARCHAR NOT NULL,
    PRIMARY KEY ("timestamp")
);

CREATE INDEX FORM_SUBMISSIONS_USER_ID_IDX ON form_submissions (user_id,form_id);
