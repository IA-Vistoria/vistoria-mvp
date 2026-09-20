-- As listagens filtram pela primeira coluna e ordenam pelas duas seguintes.
CREATE INDEX idx_vistoria_cliente_criacao_id
    ON tb_vistoria(cliente_id, data_criacao DESC, id DESC);

CREATE INDEX idx_vistoria_status_criacao_id
    ON tb_vistoria(status, data_criacao DESC, id DESC);
