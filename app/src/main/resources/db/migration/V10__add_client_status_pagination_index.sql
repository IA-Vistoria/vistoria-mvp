-- A visão de relatórios combina ownership, status e ordenação determinística.
CREATE INDEX idx_vistoria_cliente_status_criacao_id
    ON tb_vistoria(cliente_id, status, data_criacao DESC, id DESC);
