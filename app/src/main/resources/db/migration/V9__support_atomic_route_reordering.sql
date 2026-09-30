ALTER TABLE tb_ambiente_vistoria
    DROP CONSTRAINT uq_ambiente_vistoria_ordem;

ALTER TABLE tb_ambiente_vistoria
    DROP CONSTRAINT uq_ambiente_vistoria_nome;

ALTER TABLE tb_vistoria
    ADD COLUMN roteiro_revisao INTEGER NOT NULL DEFAULT 0;
