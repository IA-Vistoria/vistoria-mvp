package br.com.vistoriapredial.vistoria.application.review;

public record RevisarAchadoCommand(
        Long imagemId,
        Integer indiceAchado,
        DecisaoRevisao decisao,
        String contexto,
        String tipoCorrigido
) {
}
