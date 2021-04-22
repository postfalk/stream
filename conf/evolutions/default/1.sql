# Users schema

# --- !Ups

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL
);


# --- !Downs

DROP TABLE users;
