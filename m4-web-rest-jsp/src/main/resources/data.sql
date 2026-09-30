INSERT INTO sala (nombre, capacidad, proyector) VALUES ('Turing', 12, true);
INSERT INTO sala (nombre, capacidad, proyector) VALUES ('Lovelace', 6, false);
INSERT INTO sala (nombre, capacidad, proyector) VALUES ('Hopper', 30, true);


INSERT INTO reserva (sala_id, usuario, inicio, fin) VALUES 
(1, 'japarejo', '2024-06-01 14:00', '2024-06-01 16:00'),
(2, 'ana', '2024-06-01 09:00', '2024-06-01 11:00'),
(3, 'juan', '2024-06-03 13:00', '2024-06-03 15:00');