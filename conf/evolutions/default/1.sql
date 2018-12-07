# Users schema

# --- !Ups

CREATE TABLE users (
    id integer NOT NULL,
    name varchar(255) NOT NULL,
    token varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

# DROP TABLE users;
