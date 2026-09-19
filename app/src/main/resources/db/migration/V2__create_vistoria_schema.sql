CREATE TABLE tb_vistoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    engenheiro_id BIGINT,
    status VARCHAR(30) NOT NULL,
    pre_laudo_ia TEXT,
    parecer_engenheiro TEXT,
    data_criacao TIMESTAMP NOT NULL,
    data_conclusao TIMESTAMP,
    CONSTRAINT fk_vistoria_cliente FOREIGN KEY (cliente_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_vistoria_engenheiro FOREIGN KEY (engenheiro_id) REFERENCES tb_usuario(id)
);

CREATE TABLE tb_imagem_vistoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vistoria_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    protocolo_item VARCHAR(100) NOT NULL,
    data_upload TIMESTAMP NOT NULL,
    CONSTRAINT fk_imagem_vistoria FOREIGN KEY (vistoria_id) REFERENCES tb_vistoria(id)
);
