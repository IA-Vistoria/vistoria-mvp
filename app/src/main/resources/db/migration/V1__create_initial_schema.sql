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
