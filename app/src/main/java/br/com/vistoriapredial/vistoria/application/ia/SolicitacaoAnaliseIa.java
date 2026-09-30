package br.com.vistoriapredial.vistoria.application.ia;

import java.util.List;

public record SolicitacaoAnaliseIa(Long vistoriaId, List<EvidenciaAnaliseIa> evidencias) {

    public SolicitacaoAnaliseIa {
        if (vistoriaId == null) {
            throw new IllegalArgumentException("A vistoria deve possuir identidade.");
        }
        if (evidencias == null || evidencias.isEmpty()) {
            throw new IllegalArgumentException("A análise exige ao menos uma evidência.");
        }
        if (evidencias.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("A lista de evidências não aceita itens nulos.");
        }
        evidencias = List.copyOf(evidencias);
    }
}
