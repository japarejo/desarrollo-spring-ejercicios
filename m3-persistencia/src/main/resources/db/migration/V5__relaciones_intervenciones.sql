ALTER TABLE intervencion
    ADD COLUMN reserva_id BIGINT REFERENCES reserva (id);