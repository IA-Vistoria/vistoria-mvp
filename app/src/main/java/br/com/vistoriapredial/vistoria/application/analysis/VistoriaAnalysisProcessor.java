package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
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
        Vistoria snapshot = transactionTemplate.execute(status -> repository.findById(vistoriaId)
                .filter(vistoria -> vistoria.getStatus() == VistoriaStatus.AGUARDANDO_IA)
                .orElse(null));
        if (snapshot == null) {
            return;
        }

        List<String> imagePaths = snapshot.getImagens().stream()
                .map(ImagemVistoria::getUrl)
                .toList();
        String rawAnalysis;
        try {
            rawAnalysis = iaIntegrationService.analisarImagens(imagePaths);
            parser.parse(snapshot, rawAnalysis);
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
}
