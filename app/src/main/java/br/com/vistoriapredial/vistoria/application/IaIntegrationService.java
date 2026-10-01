package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;

import java.util.List;

public interface IaIntegrationService {

    default String analisar(SolicitacaoAnaliseIa solicitacao) {
        return analisarImagens(solicitacao.evidencias().stream()
                .map(evidencia -> evidencia.storagePath())
                .toList());
    }

    /**
     * Compatibilidade temporária dos provedores legados até a orquestração
     * contextual substituir todos os consumidores.
     */
    default String analisarImagens(List<String> imageUrls) {
        throw new UnsupportedOperationException("O provider exige contexto das evidências.");
    }
}
