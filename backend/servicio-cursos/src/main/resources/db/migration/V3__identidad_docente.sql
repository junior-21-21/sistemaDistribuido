ALTER TABLE docentes ADD COLUMN IF NOT EXISTS username VARCHAR(100);

UPDATE docentes SET username = CASE
    WHEN lower(email) = 'ana.flores@iephuamanga.edu.pe' THEN 'docente1'
    WHEN lower(email) = 'luis.palomino@iephuamanga.edu.pe' THEN 'docente2'
    WHEN lower(email) = 'elena.soto@iephuamanga.edu.pe' THEN 'coord.academica'
    ELSE username
END
WHERE username IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_docentes_username
    ON docentes(lower(username)) WHERE username IS NOT NULL;
