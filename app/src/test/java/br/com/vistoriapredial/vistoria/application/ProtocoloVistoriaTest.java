package br.com.vistoriapredial.vistoria.application;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocoloVistoriaTest {

    @Test
    void shouldExposeExactlyTheTwelveSupportedItems() {
        assertThat(ProtocoloVistoria.ITEMS).containsExactlyInAnyOrderElementsOf(Set.of(
                "SALA_PISO",
                "SALA_PAREDES_REVESTIMENTOS",
                "SALA_TETO_ILUMINACAO",
                "COZINHA_PISO",
                "COZINHA_PAREDES_BANCADAS",
                "COZINHA_INSTALACOES",
                "BANHEIRO_REVESTIMENTOS",
                "BANHEIRO_HIDRAULICA",
                "QUARTO_PISO",
                "QUARTO_PAREDES_TETO",
                "INSTALACOES_ELETRICAS",
                "INSTALACOES_HIDRAULICAS"
        ));
    }
}
