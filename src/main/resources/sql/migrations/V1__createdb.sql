CREATE TABLE users(id VARCHAR(40) PRIMARY KEY, name VARCHAR(255), email VARCHAR(320), external_id VARCHAR(40) NULL UNIQUE);
ALTER TABLE users ADD CONSTRAINT user_email_unq UNIQUE (email);

INSERT INTO users (id, name)
VALUES
('id1', 'Kim'),
('id2', 'Tim')
ON CONFLICT DO NOTHING;
