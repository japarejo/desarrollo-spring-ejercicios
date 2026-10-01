INSERT INTO sala (nombre, tipo, capacidad, proyector, email_responsable) VALUES ('Turing', 'REUNIONES', 12, true, 'turing@atech.es');
INSERT INTO sala (nombre, tipo, capacidad, proyector, email_responsable) VALUES ('Lovelace', 'REUNIONES', 6, false, 'lovelace@atech.es');
INSERT INTO sala (nombre, tipo, capacidad, proyector, email_responsable) VALUES ('Hopper', 'FORMACION', 30, true, 'formacion@atech.es');
INSERT INTO sala (nombre, tipo, capacidad, proyector, email_responsable) VALUES ('Berners-Lee', 'AUDITORIO', 150, true, 'eventos@atech.es');

INSERT INTO equipo (nombre) VALUES ('Pizarra'), ('Pantalla'), ('Videoconferencia');

INSERT INTO sala_equipo (sala_id, equipo_id) VALUES
(1, 1), (1, 3),
(3, 1), (3, 2),
(4, 2), (4, 3);

INSERT INTO reserva (sala_id, usuario, inicio, fin) VALUES
(1, 'japarejo', '2024-06-01 14:00', '2024-06-01 16:00'),
(2, 'ana', '2024-06-01 09:00', '2024-06-01 11:00'),
(3, 'juan', '2024-06-03 13:00', '2024-06-03 15:00');
