-- A fila do engenheiro filtra por status e o painel do cliente filtra por
-- cliente_id em toda consulta; nenhuma das duas colunas tinha índice.
CREATE INDEX idx_vistoria_cliente_id ON tb_vistoria(cliente_id);
CREATE INDEX idx_vistoria_status ON tb_vistoria(status);
