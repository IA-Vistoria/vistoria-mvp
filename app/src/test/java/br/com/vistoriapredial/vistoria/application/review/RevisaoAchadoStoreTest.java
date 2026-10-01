package br.com.vistoriapredial.vistoria.application.review;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RevisaoAchadoStoreTest {

    private final RevisaoAchadoStore store = new RevisaoAchadoStore(
            JsonMapper.builder().findAndAddModules().build());

    @Test
    void shouldExposeOptionalManifestationValuesWithoutRemovingLegacyValues() {
        assertThat(DecisaoRevisao.valueOf("CONCORDO")).isEqualTo(DecisaoRevisao.valueOf("CONCORDO"));
        assertThat(DecisaoRevisao.valueOf("CONTESTO")).isEqualTo(DecisaoRevisao.valueOf("CONTESTO"));
        assertThat(DecisaoRevisao.valueOf("CONTEXTO_ADICIONAL"))
                .isEqualTo(DecisaoRevisao.valueOf("CONTEXTO_ADICIONAL"));
        assertThat(DecisaoRevisao.valueOf("CONFIRMADO")).isEqualTo(DecisaoRevisao.CONFIRMADO);
        assertThat(DecisaoRevisao.valueOf("CORRIGIDO")).isEqualTo(DecisaoRevisao.CORRIGIDO);
        assertThat(DecisaoRevisao.valueOf("REJEITADO")).isEqualTo(DecisaoRevisao.REJEITADO);
    }

    @Test
    void shouldWriteVersionedDocumentAndReplaceOnlyMatchingFinding() {
        String first = store.upsert(null, new RevisaoAchado(
                20L, 0, DecisaoRevisao.CONFIRMADO, "Confirmado", null,
                Instant.parse("2026-09-30T10:00:00Z")));
        String second = store.upsert(first, new RevisaoAchado(
                20L, 1, DecisaoRevisao.REJEITADO, "Reflexo", null,
                Instant.parse("2026-09-30T10:01:00Z")));
        String replaced = store.upsert(second, new RevisaoAchado(
                20L, 0, DecisaoRevisao.CORRIGIDO, "Sombra", "Sombra",
                Instant.parse("2026-09-30T10:02:00Z")));

        assertThat(replaced).contains("\"version\":1");
        assertThat(store.read(replaced)).hasSize(2);
        assertThat(store.read(replaced).getFirst().decisao()).isEqualTo(DecisaoRevisao.CORRIGIDO);
        assertThat(store.read(replaced).get(1).decisao()).isEqualTo(DecisaoRevisao.REJEITADO);
    }
}
