package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class VistoriaAnalysisProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VistoriaAnalysisProcessor.class);

    private final VistoriaRepository repository;
    private final IaIntegrationService iaIntegrationService;
    private final PreLaudoParser parser;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public VistoriaAnalysisProcessor(
            VistoriaRepository repository,
            IaIntegrationService iaIntegrationService,
            PreLaudoParser parser,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.repository = repository;
        this.iaIntegrationService = iaIntegrationService;
        this.parser = parser;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public void process(Long vistoriaId) {
        AnalisePendente pendente;
        try {
            pendente = transactionTemplate.execute(status -> repository.findById(vistoriaId)
                    .filter(vistoria -> vistoria.getStatus() == VistoriaStatus.AGUARDANDO_IA)
                    .map(vistoria -> new AnalisePendente(
                            vistoria,
                            criarSolicitacao(vistoria)))
                    .orElse(null));
        } catch (RuntimeException exception) {
            LOGGER.warn("Contexto inválido para análise na vistoria {}: {}", vistoriaId,
                    exception.getClass().getSimpleName());
            persistFailure(vistoriaId);
            return;
        }
        if (pendente == null) {
            return;
        }

        String rawAnalysis;
        try {
            rawAnalysis = iaIntegrationService.analisar(pendente.solicitacao());
            parser.parse(pendente.snapshot(), rawAnalysis);
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha de análise na vistoria {}: {}", vistoriaId,
                    exception.getClass().getSimpleName());
            persistFailure(vistoriaId);
            return;
        }

        transactionTemplate.execute(status -> {
            repository.findById(vistoriaId)
                    .filter(vistoria -> vistoria.getStatus() == VistoriaStatus.AGUARDANDO_IA)
                    .ifPresent(vistoria -> {
                        vistoria.registrarAnalise(rawAnalysis, LocalDateTime.now(clock));
                        repository.saveAndFlush(vistoria);
                    });
            return null;
        });
    }

    private SolicitacaoAnaliseIa criarSolicitacao(Vistoria vistoria) {
        List<EvidenciaAnaliseIa> evidencias = vistoria.getImagens().stream()
                .map(this::criarEvidencia)
                .toList();
        return new SolicitacaoAnaliseIa(vistoria.getId(), evidencias);
    }

    private EvidenciaAnaliseIa criarEvidencia(ImagemVistoria imagem) {
        AmbienteVistoria ambiente = imagem.getAmbiente();
        if (ambiente == null) {
            throw new IllegalArgumentException(
                    "Toda evidência enviada à IA deve possuir ambiente.");
        }
        return new EvidenciaAnaliseIa(
                imagem.getId(),
                ambiente.getId(),
                ambiente.getNome(),
                imagem.getCategoria(),
                imagem.getUrl(),
                contentType(imagem.getUrl()));
    }

    private String contentType(String storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("O caminho da evidência é obrigatório.");
        }
        String normalized = storagePath.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".webp")) {
            return "image/webp";
        }
        throw new IllegalArgumentException(
                "O tipo da evidência não é compatível com a análise por IA.");
    }

    private void persistFailure(Long vistoriaId) {
        transactionTemplate.execute(status -> {
            repository.findById(vistoriaId)
                    .filter(vistoria -> vistoria.getStatus() == VistoriaStatus.AGUARDANDO_IA)
                    .ifPresent(vistoria -> {
                        vistoria.falharAnalise();
                        repository.saveAndFlush(vistoria);
                    });
            return null;
        });
    }

    private record AnalisePendente(
            Vistoria snapshot,
            SolicitacaoAnaliseIa solicitacao) {
    }
}
