package br.com.vistoriapredial.vistoria.application.ia;

import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;

public record EvidenciaAnaliseIa(
        Long imagemId,
        Long ambienteId,
        String ambienteNome,
        CategoriaEvidencia categoria,
        String storagePath,
        String contentType) {

    public EvidenciaAnaliseIa {
        if (imagemId == null || ambienteId == null) {
            throw new IllegalArgumentException("Imagem e ambiente devem possuir identidade.");
        }
        ambienteNome = textoObrigatorio(ambienteNome, "O nome do ambiente é obrigatório.");
        if (categoria == null) {
            throw new IllegalArgumentException("A categoria da evidência é obrigatória.");
        }
        storagePath = textoObrigatorio(storagePath, "O caminho da evidência é obrigatório.");
        contentType = textoObrigatorio(contentType, "O tipo de conteúdo é obrigatório.");
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.strip();
    }
}
