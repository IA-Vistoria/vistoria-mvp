package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VistoriaAnalysisProcessorTest {

    private static final LocalDateTime CONCLUSAO = LocalDateTime.of(2026, 9, 30, 20, 0);

    @Mock
    private VistoriaRepository repository;

    @Mock
    private IaIntegrationService iaIntegrationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final AtomicBoolean transacaoAtiva = new AtomicBoolean();
    private VistoriaAnalysisProcessor processor;
    private Vistoria vistoria;

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            transacaoAtiva.set(true);
            try {
                return callback.doInTransaction(new SimpleTransactionStatus());
            } finally {
                transacaoAtiva.set(false);
            }
        });
        processor = new VistoriaAnalysisProcessor(
                repository,
                iaIntegrationService,
                new PreLaudoParser(new ObjectMapper()),
                transactionTemplate,
                Clock.fixed(Instant.parse("2026-09-30T20:00:00Z"), ZoneOffset.UTC));
        vistoria = vistoriaPendente("uploads/sala.jpg", CategoriaEvidencia.VISAO_GERAL);
        when(repository.findById(10L)).thenReturn(Optional.of(vistoria));
    }

    @Test
    void deveDisponibilizarRelatorioDepoisDeAnaliseContextualValida() {
        String raw = analiseValida();
        when(iaIntegrationService.analisar(any())).thenReturn(raw);

        processor.process(10L);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
        assertThat(vistoria.getPreLaudoIa()).isEqualTo(raw);
        assertThat(vistoria.getDataConclusao()).isEqualTo(CONCLUSAO);
        verify(repository).saveAndFlush(vistoria);
    }

    @Test
    void devePreservarImagemAmbienteCategoriaECaminhoNaSolicitacao() {
        when(iaIntegrationService.analisar(any())).thenReturn(analiseValida());
        ArgumentCaptor<SolicitacaoAnaliseIa> captor =
                ArgumentCaptor.forClass(SolicitacaoAnaliseIa.class);

        processor.process(10L);

        verify(iaIntegrationService).analisar(captor.capture());
        SolicitacaoAnaliseIa solicitacao = captor.getValue();
        assertThat(solicitacao.vistoriaId()).isEqualTo(10L);
        assertThat(solicitacao.evidencias()).singleElement().satisfies(evidencia -> {
            assertThat(evidencia.imagemId()).isEqualTo(20L);
            assertThat(evidencia.ambienteId()).isEqualTo(30L);
            assertThat(evidencia.ambienteNome()).isEqualTo("Sala integrada");
            assertThat(evidencia.categoria()).isEqualTo(CategoriaEvidencia.VISAO_GERAL);
            assertThat(evidencia.storagePath()).isEqualTo("uploads/sala.jpg");
            assertThat(evidencia.contentType()).isEqualTo("image/jpeg");
        });
    }

    @Test
    void deveExecutarChamadaExternaForaDaTransacao() {
        when(iaIntegrationService.analisar(any())).thenAnswer(invocation -> {
            assertThat(transacaoAtiva).isFalse();
            return analiseValida();
        });

        processor.process(10L);

        verify(iaIntegrationService).analisar(any());
    }

    @Test
    void devePersistirFalhaSemDocumentoFabricadoQuandoProviderFalha() {
        when(iaIntegrationService.provedor()).thenReturn("oci");
        when(iaIntegrationService.modelo()).thenReturn("google.gemini-2.5-flash");
        when(iaIntegrationService.analisar(any()))
                .thenThrow(new IllegalStateException("provider unavailable"));

        Logger logger = (Logger) LoggerFactory.getLogger(VistoriaAnalysisProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            processor.process(10L);

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(logs)
                    .contains("provedor=oci")
                    .contains("modelo=google.gemini-2.5-flash")
                    .contains("categoria=FALHA_NAO_CLASSIFICADA")
                    .contains("duracaoMs=")
                    .doesNotContain("provider unavailable");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
        assertThat(vistoria.getDataConclusao()).isNull();
        verify(repository).saveAndFlush(vistoria);
    }

    @Test
    void devePersistirFalhaQuandoProviderRetornaContratoInvalido() {
        when(iaIntegrationService.analisar(any()))
                .thenReturn("{\"version\":2,\"images\":[]}");

        processor.process(10L);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
        verify(repository).saveAndFlush(vistoria);
    }

    @Test
    void deveFalharAntesDoProviderQuandoEvidenciaNaoPossuiContexto() {
        Vistoria semContexto = new Vistoria();
        semContexto.setId(10L);
        semContexto.setStatus(VistoriaStatus.AGUARDANDO_IA);
        ImagemVistoria imagem = new ImagemVistoria();
        imagem.setId(20L);
        imagem.setUrl("uploads/legada.jpg");
        semContexto.getImagens().add(imagem);
        when(repository.findById(10L)).thenReturn(Optional.of(semContexto));

        processor.process(10L);

        verify(iaIntegrationService, never()).analisar(any());
        assertThat(semContexto.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(semContexto.getPreLaudoIa()).isNull();
    }

    @Test
    void deveFalharAntesDoProviderQuandoExtensaoNaoIdentificaTipoSuportado() {
        Vistoria tipoDesconhecido =
                vistoriaPendente("uploads/sala.bin", CategoriaEvidencia.VISAO_GERAL);
        when(repository.findById(10L)).thenReturn(Optional.of(tipoDesconhecido));

        processor.process(10L);

        verify(iaIntegrationService, never()).analisar(any());
        assertThat(tipoDesconhecido.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
    }

    @Test
    void deveIgnorarEventoQuandoVistoriaNaoEstaMaisPendente() {
        vistoria.setStatus(VistoriaStatus.FALHA_IA);

        processor.process(10L);

        verify(iaIntegrationService, never()).analisar(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void execucaoDuplicadaNaoDeveChamarProviderNemSobrescreverRelatorioConcluido() {
        String raw = analiseValida();
        when(iaIntegrationService.analisar(any())).thenReturn(raw);

        processor.process(10L);
        processor.process(10L);

        verify(iaIntegrationService, times(1)).analisar(any());
        verify(repository, times(1)).saveAndFlush(vistoria);
        assertThat(vistoria.getPreLaudoIa()).isEqualTo(raw);
    }

    @Test
    void resultadoAtrasadoNaoDeveSobrescreverRelatorioConcluidoPorOutraExecucao() {
        Vistoria concluida = vistoriaPendente("uploads/sala.jpg", CategoriaEvidencia.VISAO_GERAL);
        concluida.registrarAnalise("documento anterior", CONCLUSAO.minusMinutes(1));
        when(repository.findById(10L))
                .thenReturn(Optional.of(vistoria), Optional.of(concluida));
        when(iaIntegrationService.analisar(any())).thenReturn(analiseValida());

        Logger logger = (Logger) LoggerFactory.getLogger(VistoriaAnalysisProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            processor.process(10L);

            assertThat(appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage))
                    .anyMatch(message -> message.contains("categoria=RESULTADO_DESCARTADO"))
                    .noneMatch(message -> message.startsWith("Análise concluída"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(repository, never()).saveAndFlush(any());
        assertThat(concluida.getPreLaudoIa()).isEqualTo("documento anterior");
        assertThat(concluida.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
    }

    @Test
    void falhaAtrasadaNaoDeveApagarRelatorioConcluidoPorOutraExecucao() {
        Vistoria concluida = vistoriaPendente("uploads/sala.jpg", CategoriaEvidencia.VISAO_GERAL);
        concluida.registrarAnalise("documento anterior", CONCLUSAO.minusMinutes(1));
        when(repository.findById(10L))
                .thenReturn(Optional.of(vistoria), Optional.of(concluida));
        when(iaIntegrationService.analisar(any()))
                .thenThrow(new IllegalStateException("timeout"));

        processor.process(10L);

        verify(repository, never()).saveAndFlush(any());
        assertThat(concluida.getPreLaudoIa()).isEqualTo("documento anterior");
        assertThat(concluida.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
    }

    private Vistoria vistoriaPendente(String path, CategoriaEvidencia categoria) {
        Vistoria result = new Vistoria();
        result.setId(10L);
        result.setStatus(VistoriaStatus.AGUARDANDO_IA);
        AmbienteVistoria ambiente = AmbienteVistoria.criar(
                TipoAmbiente.SALA, "Sala integrada", 0);
        result.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(ambiente));
        ReflectionTestUtils.setField(ambiente, "id", 30L);
        ImagemVistoria imagem = new ImagemVistoria(
                result,
                ambiente,
                categoria,
                path,
                LocalDateTime.of(2026, 9, 30, 19, 30));
        imagem.setId(20L);
        result.getImagens().add(imagem);
        return result;
    }

    private String analiseValida() {
        return """
                {
                  "version": 2,
                  "execution": {
                    "provider": "oci",
                    "model": "google.gemini-2.5-flash",
                    "promptVersion": "oci-gemini-evidencia-v1",
                    "analysisId": "ana-1",
                    "completedAt": "2026-09-30T20:00:00Z"
                  },
                  "images": [{
                    "imageId": 20,
                    "storagePath": "uploads/sala.jpg",
                    "environment": {
                      "id": 30,
                      "name": "Sala integrada",
                      "category": "VISAO_GERAL"
                    },
                    "imageQuality": "SUFICIENTE",
                    "summary": "A imagem permite avaliar visualmente o ambiente.",
                    "limitations": [],
                    "captureGuidance": null,
                    "findings": []
                  }],
                  "environments": [{
                    "id": 30,
                    "name": "Sala integrada",
                    "result": "APROVADO",
                    "resultReason": "As evidências suficientes não apresentaram achados visuais."
                  }],
                  "overallResult": "APROVADO",
                  "overallReason": "As evidências suficientes não apresentaram achados visuais."
                }
                """;
    }
}
