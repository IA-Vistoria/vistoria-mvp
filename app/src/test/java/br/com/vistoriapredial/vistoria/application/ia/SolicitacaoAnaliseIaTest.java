package br.com.vistoriapredial.vistoria.application.ia;

import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitacaoAnaliseIaTest {

    @Test
    void rejeitaListaNulaOuVazia() {
        assertThrows(IllegalArgumentException.class, () -> new SolicitacaoAnaliseIa(7L, null));
        assertThrows(IllegalArgumentException.class, () -> new SolicitacaoAnaliseIa(7L, List.of()));
    }

    @Test
    void rejeitaVistoriaSemIdentidade() {
        EvidenciaAnaliseIa evidencia = evidenciaValida();

        assertThrows(IllegalArgumentException.class, () -> new SolicitacaoAnaliseIa(null, List.of(evidencia)));
    }

    @Test
    void rejeitaEvidenciaSemIdentidades() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                null, 11L, "Banheiro", CategoriaEvidencia.VISAO_GERAL,
                "vistorias/7/banheiro.jpg", "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                31L, null, "Banheiro", CategoriaEvidencia.VISAO_GERAL,
                "vistorias/7/banheiro.jpg", "image/jpeg"));
    }

    @Test
    void rejeitaEvidenciaSemContextoObrigatorio() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                31L, 11L, " ", CategoriaEvidencia.VISAO_GERAL,
                "vistorias/7/banheiro.jpg", "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                31L, 11L, "Banheiro", null,
                "vistorias/7/banheiro.jpg", "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                31L, 11L, "Banheiro", CategoriaEvidencia.VISAO_GERAL,
                " ", "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () -> new EvidenciaAnaliseIa(
                31L, 11L, "Banheiro", CategoriaEvidencia.VISAO_GERAL,
                "vistorias/7/banheiro.jpg", " "));
    }

    @Test
    void preservaContextoFornecidoPeloDominio() {
        EvidenciaAnaliseIa evidencia = evidenciaValida();
        SolicitacaoAnaliseIa solicitacao = new SolicitacaoAnaliseIa(7L, List.of(evidencia));

        assertEquals(7L, solicitacao.vistoriaId());
        assertEquals(31L, solicitacao.evidencias().getFirst().imagemId());
        assertEquals(11L, solicitacao.evidencias().getFirst().ambienteId());
        assertEquals("Banheiro social", solicitacao.evidencias().getFirst().ambienteNome());
        assertEquals(CategoriaEvidencia.VISAO_GERAL, solicitacao.evidencias().getFirst().categoria());
        assertEquals("vistorias/7/banheiro.jpg", solicitacao.evidencias().getFirst().storagePath());
        assertEquals("image/jpeg", solicitacao.evidencias().getFirst().contentType());
    }

    @Test
    void protegeListaContraAlteracaoPosterior() {
        var evidencias = new java.util.ArrayList<>(List.of(evidenciaValida()));
        SolicitacaoAnaliseIa solicitacao = new SolicitacaoAnaliseIa(7L, evidencias);

        evidencias.clear();

        assertEquals(1, solicitacao.evidencias().size());
        assertThrows(UnsupportedOperationException.class,
                () -> solicitacao.evidencias().add(evidenciaValida()));
    }

    private EvidenciaAnaliseIa evidenciaValida() {
        return new EvidenciaAnaliseIa(
                31L,
                11L,
                "Banheiro social",
                CategoriaEvidencia.VISAO_GERAL,
                "vistorias/7/banheiro.jpg",
                "image/jpeg");
    }
}
