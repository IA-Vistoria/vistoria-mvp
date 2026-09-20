package br.com.vistoriapredial.vistoria.application;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocoloVistoriaTest {

    @Test
    void shouldExposeOnlyWallItemForMvp() {
        assertThat(ProtocoloVistoria.ITEMS).containsExactlyInAnyOrderElementsOf(Set.of(
                "SALA_PAREDES_REVESTIMENTOS"
        ));
    }
}
