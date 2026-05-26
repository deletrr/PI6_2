CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE vagas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    localizacao GEOMETRY(Point, 4326) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LIVRE'
);

CREATE TABLE hardware_esp32 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vaga_id UUID NOT NULL UNIQUE REFERENCES vagas(id),
    device_id VARCHAR(100) NOT NULL UNIQUE,
    hmac_secret VARCHAR(255) NOT NULL,
    firmware_version VARCHAR(50) NOT NULL,
    battery_level INT NOT NULL DEFAULT 100,
    last_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_vagas_localizacao ON vagas USING GIST(localizacao);
CREATE INDEX idx_vagas_status ON vagas(status);
CREATE INDEX idx_hardware_device_id ON hardware_esp32(device_id);
