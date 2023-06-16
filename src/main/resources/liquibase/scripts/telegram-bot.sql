-- liquibase formatted sql

-- changeset sgots:1
CREATE TABLE notification (
    id          INT8 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id     INT8 REFERENCES chats(id),
    target_time TIMESTAMP NOT NULL CHECK (target_time > CURRENT_TIMESTAMP),
    message     VARCHAR(255) NOT NULL
);

CREATE TABLE chats (
    id              INT8 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_first_name VARCHAR(32) NOT NULL,
    state           INT2 NOT NULL,
    target_time     TIME,
    target_date     DATE
);

-- changeset sgots:2
ALTER TABLE chats ADD COLUMN lang INT2 DEFAULT(0);