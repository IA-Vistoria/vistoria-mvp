package br.com.vistoriapredial.vistoria.application;

import java.util.Set;

public final class ProtocoloVistoria {

    /** MVP: apenas evidências de paredes. */
    public static final Set<String> ITEMS = Set.of(
            "SALA_PAREDES_REVESTIMENTOS"
    );

    private ProtocoloVistoria() {
    }
}
