# Add autoincrement

# --- !Ups

CREATE SEQUENCE users_id_seq;
SELECT setval('users_id_seq', (SELECT MAX(id) + 1 FROM users));
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_id_seq');

# --- !Downs

ALTER TABLE users ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE users_id_seq;
