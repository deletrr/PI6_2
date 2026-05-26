CREATE TABLE infracoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vaga_id UUID NOT NULL REFERENCES vagas(id),
    fiscal_id VARCHAR(100) NOT NULL,
    foto_hash CHAR(64) NOT NULL,
    localizacao GEOMETRY(Point, 4326) NOT NULL,
    registrada_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vaga_id UUID NOT NULL REFERENCES vagas(id),
    motorista_cpf VARCHAR(14) NOT NULL,
    placa VARCHAR(10) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em TIMESTAMPTZ NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    status_pagamento VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
);

CREATE INDEX idx_pagamentos_vaga_expira ON pagamentos(vaga_id, expira_em);
CREATE INDEX idx_infracoes_vaga ON infracoes(vaga_id);
