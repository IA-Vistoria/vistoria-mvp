package br.com.vistoriapredial.vistoria.application.review;

import java.time.Instant;

public record RevisaoAchado(
        long imagemId,
        int indiceAchado,
        DecisaoRevisao decisao,
        String contexto,
        String tipoCorrigido,
        Instant revisadoEm
) {
    public boolean matches(long imageId, int findingIndex) {
        return imagemId == imageId && indiceAchado == findingIndex;
    }
}
