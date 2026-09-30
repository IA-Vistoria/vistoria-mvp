package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VistoriaAnalysisProcessorTest {

    @Mock
    private VistoriaRepository repository;

    @Mock
    private IaIntegrationService iaIntegrationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private VistoriaAnalysisProcessor processor;
    private Vistoria vistoria;

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        processor = new VistoriaAnalysisProcessor(
                repository,
                iaIntegrationService,
                new PreLaudoParser(new ObjectMapper()),
                transactionTemplate);
        vistoria = pendingInspection();
        when(repository.findById(10L)).thenReturn(Optional.of(vistoria));
    }

    @Test
    void shouldPersistValidAnalysisAsPendingReview() {
        String raw = validAnalysis();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(iaIntegrationService.analisarImagens(java.util.List.of("uploads/a.jpg"))).thenReturn(raw);

        processor.process(10L);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.REVISAO_PENDENTE);
        assertThat(vistoria.getPreLaudoIa()).isEqualTo(raw);
        assertThat(vistoria.getDataConclusao()).isNull();
        verify(repository).saveAndFlush(vistoria);
    }

    @Test
    void shouldPersistFailureWithoutFabricatedAnalysisWhenProviderFails() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(iaIntegrationService.analisarImagens(java.util.List.of("uploads/a.jpg")))
                .thenThrow(new IllegalStateException("provider unavailable"));

        processor.process(10L);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
        assertThat(vistoria.getDataConclusao()).isNull();
    }

    @Test
    void shouldPersistFailureWhenProviderReturnsInvalidContract() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(iaIntegrationService.analisarImagens(java.util.List.of("uploads/a.jpg")))
                .thenReturn("{\"version\":1,\"images\":[{\"storagePath\":\"unknown.jpg\"}]}");

        processor.process(10L);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
    }

    @Test
    void shouldIgnoreEventWhenInspectionIsNoLongerPending() {
        vistoria.setStatus(VistoriaStatus.FALHA_IA);

        processor.process(10L);

        verify(iaIntegrationService, never()).analisarImagens(any());
        verify(repository, never()).saveAndFlush(any());
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
    }

    private Vistoria pendingInspection() {
        Vistoria result = new Vistoria();
        result.setId(10L);
        result.setStatus(VistoriaStatus.AGUARDANDO_IA);
        ImagemVistoria image = new ImagemVistoria();
        image.setId(20L);
        image.setUrl("uploads/a.jpg");
        result.getImagens().add(image);
        return result;
    }

    private String validAnalysis() {
        return """
                {"version":1,"images":[{
                  "storagePath":"uploads/a.jpg",
                  "analysisId":"ana-1",
                  "overallSummary":"Marca visual.",
                  "limitations":[],
                  "imageQuality":{"usable":true,"issues":[]},
                  "areas":[]
                }]}
                """;
    }
}
