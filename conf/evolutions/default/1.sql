# Users schema

# --- !Ups
CREATE TABLE Users (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL
);

# --- !Downs
DROP TABLE User;
