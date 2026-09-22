-- EJ 3.5 - Evolución del esquema: nueva columna, índice y datos de ejemplo
ALTER TABLE reserva ADD COLUMN notas VARCHAR(500);

CREATE INDEX idx_reserva_sala_inicio ON reserva (sala_id, inicio);

INSERT INTO sala (nombre, capacidad, calle, ciudad, codigo_postal) VALUES
    ('Turing',   12, 'Av. Reina Mercedes s/n', 'Sevilla', '41012'),
    ('Lovelace',  6, 'Av. Reina Mercedes s/n', 'Sevilla', '41012'),
    ('Hopper',   30, 'Calle de Alcalá 1',      'Madrid',  '28014');

INSERT INTO usuario (email, nombre) VALUES
    ('ana@atech.es',   'Ana'),
    ('luis@atech.es',  'Luis'),
    ('marta@atech.es', 'Marta');

INSERT INTO reserva (sala_id, usuario_id, inicio, fin, estado, version) VALUES
    ((SELECT id FROM sala WHERE nombre = 'Turing'),   (SELECT id FROM usuario WHERE email = 'ana@atech.es'),
        TIMESTAMP '2030-01-10 09:00:00', TIMESTAMP '2030-01-10 11:00:00', 'CONFIRMADA', 0),
    ((SELECT id FROM sala WHERE nombre = 'Turing'),   (SELECT id FROM usuario WHERE email = 'luis@atech.es'),
        TIMESTAMP '2030-01-10 12:00:00', TIMESTAMP '2030-01-10 13:00:00', 'CONFIRMADA', 0),
    ((SELECT id FROM sala WHERE nombre = 'Lovelace'), (SELECT id FROM usuario WHERE email = 'ana@atech.es'),
        TIMESTAMP '2030-01-11 10:00:00', TIMESTAMP '2030-01-11 12:00:00', 'CONFIRMADA', 0),
    ((SELECT id FROM sala WHERE nombre = 'Hopper'),   (SELECT id FROM usuario WHERE email = 'marta@atech.es'),
        TIMESTAMP '2030-01-12 16:00:00', TIMESTAMP '2030-01-12 18:00:00', 'CONFIRMADA', 0),
    ((SELECT id FROM sala WHERE nombre = 'Hopper'),   (SELECT id FROM usuario WHERE email = 'luis@atech.es'),
        TIMESTAMP '2030-01-13 09:00:00', TIMESTAMP '2030-01-13 10:00:00', 'CANCELADA', 0);
