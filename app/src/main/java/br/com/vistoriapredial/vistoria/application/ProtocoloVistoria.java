package br.com.vistoriapredial.vistoria.application;

import java.util.Set;

public final class ProtocoloVistoria {

    public static final Set<String> ITEMS = Set.of(
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
    );

    private ProtocoloVistoria() {
    }
}
