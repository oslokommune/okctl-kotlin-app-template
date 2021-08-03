INSERT INTO users (id, name)
VALUES
('id3', 'Tom'),
('id4', 'Timmy')
ON CONFLICT DO NOTHING;
