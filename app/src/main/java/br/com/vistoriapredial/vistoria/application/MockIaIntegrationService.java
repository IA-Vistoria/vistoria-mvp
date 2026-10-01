package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory.MetadadosExecucao;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory.ObservacaoAchado;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory.ObservacaoImagem;
import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile({"test", "demo"})
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "mock")
public class MockIaIntegrationService implements IaIntegrationService {

    private static final String LIMITACAO_DEMO =
            "A execução em modo demo usa dados predefinidos e não interpreta o conteúdo real da foto.";

    private final AnaliseVistoriaDocumentFactory documentFactory;
    private final Clock clock;

    public MockIaIntegrationService(
            AnaliseVistoriaDocumentFactory documentFactory,
            Clock clock) {
        this.documentFactory = documentFactory;
        this.clock = clock;
    }

    @Override
    public String analisar(SolicitacaoAnaliseIa solicitacao) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("A solicitação de análise é obrigatória.");
        }
        if (solicitacao.evidencias().stream()
                .map(EvidenciaAnaliseIa::storagePath)
                .anyMatch(path -> path.contains("trigger-fail"))) {
            throw new IllegalStateException("Falha simulada do provedor de IA.");
        }

        List<ObservacaoImagem> observacoes = new ArrayList<>();
        for (int index = 0; index < solicitacao.evidencias().size(); index++) {
            observacoes.add(criarCenario(solicitacao.evidencias().get(index), index));
        }

        return documentFactory.criar(
                solicitacao,
                new MetadadosExecucao(
                        "mock",
                        "fixture-visual-v2",
                        "vistoria-demo-v2",
                        "mock-demo-" + solicitacao.vistoriaId(),
                        Instant.now(clock)),
                observacoes);
    }

    private ObservacaoImagem criarCenario(EvidenciaAnaliseIa evidencia, int index) {
        return switch (index % 4) {
            case 0 -> cenarioUmidade(evidencia);
            case 1 -> cenarioFissura(evidencia);
            case 2 -> cenarioSemAchados(evidencia);
            default -> cenarioInconclusivo(evidencia);
        };
    }

    private ObservacaoImagem cenarioUmidade(EvidenciaAnaliseIa evidencia) {
        return new ObservacaoImagem(
                evidencia.storagePath(),
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                "Cenário demonstrativo: foram simulados indícios visuais de umidade ou mofo em "
                        + evidencia.ambienteNome() + ".",
                List.of(LIMITACAO_DEMO, "A origem e a extensão da umidade exigem avaliação presencial."),
                null,
                List.of(new ObservacaoAchado(
                        "Integridade aparente do revestimento",
                        "parede",
                        "UMIDADE_OU_MOFO_APARENTE",
                        "Foram simuladas manchas escuras e irregulares na superfície.",
                        "Distribuição irregular simulada no acabamento da parede.",
                        "O padrão pode estar associado à degradação do revestimento e requer investigação da origem.",
                        AnaliseVistoria.GravidadeAchado.ALTA,
                        AnaliseVistoria.ConfiancaAchado.ALTA,
                        "Verificar a origem da umidade e realizar avaliação presencial antes de qualquer reparo.",
                        "Parede principal do ambiente (simulação)")));
    }

    private ObservacaoImagem cenarioFissura(EvidenciaAnaliseIa evidencia) {
        return new ObservacaoImagem(
                evidencia.storagePath(),
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                "Cenário demonstrativo: foi simulada uma fissura aparente em "
                        + evidencia.ambienteNome() + ".",
                List.of(LIMITACAO_DEMO, "A profundidade e a atividade da fissura não podem ser medidas por fotografia."),
                null,
                List.of(new ObservacaoAchado(
                        "Continuidade aparente da superfície",
                        "parede",
                        "FISSURA_APARENTE",
                        "Foi simulada uma linha fina e contínua no acabamento.",
                        "Traço linear simulado próximo ao encontro entre parede e teto.",
                        "O indício pode representar movimentação do acabamento e deve ser acompanhado.",
                        AnaliseVistoria.GravidadeAchado.MEDIA,
                        AnaliseVistoria.ConfiancaAchado.MEDIA,
                        "Registrar a evolução e solicitar avaliação presencial se houver aumento ou recorrência.",
                        "Encontro entre parede e teto (simulação)")));
    }

    private ObservacaoImagem cenarioSemAchados(EvidenciaAnaliseIa evidencia) {
        return new ObservacaoImagem(
                evidencia.storagePath(),
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                "Cenário demonstrativo: a evidência de " + evidencia.ambienteNome()
                        + " foi classificada sem indícios visuais aparentes.",
                List.of(LIMITACAO_DEMO),
                null,
                List.of());
    }

    private ObservacaoImagem cenarioInconclusivo(EvidenciaAnaliseIa evidencia) {
        return new ObservacaoImagem(
                evidencia.storagePath(),
                AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE,
                "Cenário demonstrativo: a evidência de " + evidencia.ambienteNome()
                        + " foi classificada como insuficiente para uma conclusão segura.",
                List.of(LIMITACAO_DEMO, "Baixa iluminação simulada reduziu a leitura da superfície."),
                "Refaça a visão geral com iluminação uniforme e mantendo piso, paredes e teto visíveis.",
                List.of());
    }
}
