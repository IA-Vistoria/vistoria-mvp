-- Migração inicial: estrutura de tabelas do sistema de vistoria predial
-- Compatível com H2 (dev local) e PostgreSQL (produção)

CREATE TABLE tb_usuario (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY,
    nome       VARCHAR(150) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha      VARCHAR(255) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    crea       VARCHAR(30),
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    version    BIGINT      NOT NULL,
    CONSTRAINT pk_tb_usuario PRIMARY KEY (id),
    CONSTRAINT uk_tb_usuario_email UNIQUE (email)
);

CREATE TABLE tb_vistoria (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY,
    usuario_id   BIGINT       NOT NULL,
    comodo       VARCHAR(100) NOT NULL,
    descricao    TEXT,
    foto_url     VARCHAR(512),
    status       VARCHAR(30)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    version      BIGINT       NOT NULL,
    CONSTRAINT pk_tb_vistoria PRIMARY KEY (id),
    CONSTRAINT fk_vistoria_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario (id)
);

CREATE TABLE tb_laudo (
    id                  BIGINT  GENERATED ALWAYS AS IDENTITY,
    vistoria_id         BIGINT  NOT NULL,
    parecer_ia          TEXT,
    parecer_engenheiro  TEXT,
    aprovado            BOOLEAN,
    status              VARCHAR(20) NOT NULL,
    data_homologacao    TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    version             BIGINT    NOT NULL,
    CONSTRAINT pk_tb_laudo PRIMARY KEY (id),
    CONSTRAINT fk_laudo_vistoria FOREIGN KEY (vistoria_id) REFERENCES tb_vistoria (id)
);

CREATE INDEX idx_vistoria_usuario_id ON tb_vistoria (usuario_id);
CREATE INDEX idx_vistoria_status ON tb_vistoria (status);
CREATE INDEX idx_laudo_vistoria_id ON tb_laudo (vistoria_id);
