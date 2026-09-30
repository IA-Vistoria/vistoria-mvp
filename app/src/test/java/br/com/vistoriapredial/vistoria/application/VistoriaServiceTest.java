package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import br.com.vistoriapredial.vistoria.application.exception.IncompleteInspectionException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import br.com.vistoriapredial.vistoria.application.analysis.VistoriaSubmetidaEvent;
import br.com.vistoriapredial.vistoria.application.analysis.PreLaudoParser;
import br.com.vistoriapredial.vistoria.application.exception.FindingNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.IncompleteReviewException;
import br.com.vistoriapredial.vistoria.application.exception.InvalidReviewException;
import br.com.vistoriapredial.vistoria.application.review.DecisaoRevisao;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchadoStore;
import br.com.vistoriapredial.vistoria.application.review.RevisarAchadoCommand;
import br.com.vistoriapredial.vistoria.application.command.AmbienteRoteiroCommand;
import br.com.vistoriapredial.vistoria.application.command.CriarVistoriaCommand;
import br.com.vistoriapredial.vistoria.application.command.RegistrarEvidenciaCommand;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class VistoriaServiceTest {

    @Mock
    private VistoriaRepository vistoriaRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private EvidenceFileValidator evidenceFileValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VistoriaService vistoriaService;

    private Usuario cliente;
    private Usuario engenheiro;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        cliente = new Usuario("Cliente", "client@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(cliente, "id", 1L);

        engenheiro = new Usuario("Eng", "eng@test.com", "pass", PerfilEnum.ROLE_ENGENHEIRO, "1234");
        ReflectionTestUtils.setField(engenheiro, "id", 2L);

        var objectMapper = JsonMapper.builder().findAndAddModules().build();
        vistoriaService = new VistoriaService(
                vistoriaRepository,
                storageService,
                evidenceFileValidator,
                eventPublisher,
                new PreLaudoParser(objectMapper),
                new RevisaoAchadoStore(objectMapper),
                Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC));

    }

    @Test
    void shouldCreateVistoriaIfCliente() {
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria result = vistoriaService.criarVistoria(cliente, new CriarVistoriaCommand(
                "Endereço Teste",
                TipoImovel.CASA,
                List.of(new AmbienteRoteiroCommand(null, TipoAmbiente.SALA, "Sala"))));
        
        assertNotNull(result);
        assertEquals(cliente, result.getCliente());
        assertEquals(VistoriaStatus.EM_RASCUNHO, result.getStatus());
        assertEquals("Endereço Teste", result.getEndereco());
        assertThat(result.getAmbientes()).hasSize(1);
    }

    @Test
    void shouldListarVistoriasClienteComPaginacaoEImagensCarregadas() {
        Vistoria semImagens = new Vistoria();
        semImagens.setId(10L);
        semImagens.setCliente(cliente);
        Pageable pageable = PageRequest.of(0, 10);
        when(vistoriaRepository.findByCliente(cliente, pageable))
                .thenReturn(new PageImpl<>(List.of(semImagens), pageable, 1));

        Vistoria comImagens = editableInspection();
        comImagens.getImagens().add(evidence("uploads/a.jpg"));
        when(vistoriaRepository.findByIdIn(List.of(10L))).thenReturn(List.of(comImagens));

        Page<Vistoria> pagina = vistoriaService.listarVistoriasCliente(cliente, pageable);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).containsExactly(comImagens);
        assertThat(pagina.getContent().getFirst().getImagens()).hasSize(1);
    }

    @Test
    void shouldListarVistoriasClientePorStatusComPaginacao() {
        Vistoria relatorio = inspectionWithEvidence(VistoriaStatus.RELATORIO_DISPONIVEL);
        Pageable pageable = PageRequest.of(0, 10);
        when(vistoriaRepository.findByClienteAndStatus(
                cliente, VistoriaStatus.RELATORIO_DISPONIVEL, pageable))
                .thenReturn(new PageImpl<>(List.of(relatorio), pageable, 1));
        when(vistoriaRepository.findByIdIn(List.of(10L))).thenReturn(List.of(relatorio));

        Page<Vistoria> pagina = vistoriaService.listarVistoriasClientePorStatus(
                cliente, VistoriaStatus.RELATORIO_DISPONIVEL, pageable);

        assertThat(pagina.getContent()).containsExactly(relatorio);
        verify(vistoriaRepository).findByClienteAndStatus(
                cliente, VistoriaStatus.RELATORIO_DISPONIVEL, pageable);
    }

    @Test
    void shouldRejectListarVistoriasClienteForNonCliente() {
        assertThatThrownBy(() -> vistoriaService.listarVistoriasCliente(engenheiro, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldListarPendentesEngenhariaComPaginacao() {
        Vistoria pendente = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        Pageable pageable = PageRequest.of(0, 10);
        when(vistoriaRepository.findByStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO, pageable))
                .thenReturn(new PageImpl<>(List.of(pendente), pageable, 1));
        when(vistoriaRepository.findByIdIn(List.of(10L))).thenReturn(List.of(pendente));

        Page<Vistoria> pagina = vistoriaService.listarPendentesEngenharia(engenheiro, pageable);

        assertThat(pagina.getContent()).containsExactly(pendente);
    }

    @Test
    void shouldBuscarVistoriaForOwner() {
        Vistoria vistoria = editableInspection();
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThat(vistoriaService.buscarVistoria(10L, cliente)).isSameAs(vistoria);
    }

    @Test
    void shouldBuscarVistoriaForEngineerWhilePending() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThat(vistoriaService.buscarVistoria(10L, engenheiro)).isSameAs(vistoria);
    }

    @Test
    void shouldDenyBuscarVistoriaForAnotherClient() {
        Usuario outroCliente = new Usuario("Outro", "outro3@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(outroCliente, "id", 77L);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(editableInspection()));

        assertThatThrownBy(() -> vistoriaService.buscarVistoria(10L, outroCliente))
                .isInstanceOf(VistoriaAccessDeniedException.class);
    }

    @Test
    void shouldDenyBuscarVistoriaForEngineerWhenNotPending() {
        when(vistoriaRepository.findById(10L))
                .thenReturn(Optional.of(inspectionWithEvidence(VistoriaStatus.CONCLUIDA)));

        assertThatThrownBy(() -> vistoriaService.buscarVistoria(10L, engenheiro))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldThrowVistoriaNotFoundWhenBuscandoVistoriaInexistente() {
        when(vistoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vistoriaService.buscarVistoria(999L, cliente))
                .isInstanceOf(VistoriaNotFoundException.class);
    }

    @Test
    void shouldUploadImagem() {
        Vistoria v = inspectionWithRoute("Sala");
        AmbienteVistoria sala = v.getAmbientes().getFirst();

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        MockMultipartFile file = new MockMultipartFile("file", "nome-do-cliente.jpg", "image/jpeg", "test data".getBytes());
        when(evidenceFileValidator.validate(file))
                .thenReturn(new ValidatedEvidence(".jpg", MediaType.IMAGE_JPEG));
        when(storageService.store(eq(file), anyString())).thenReturn("uploads/a.jpg");
        when(vistoriaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria result = vistoriaService.uploadImagem(
                10L, cliente, new RegistrarEvidenciaCommand(11L, "VISAO_GERAL"), file);

        assertThat(result).isSameAs(v);
        assertEquals(1, v.getImagens().size());
        assertThat(v.getImagens().getFirst().getUrl()).isEqualTo("uploads/a.jpg");
        assertThat(v.getImagens().getFirst().getAmbiente()).isSameAs(sala);
        assertThat(v.getImagens().getFirst().getCategoria()).isEqualTo(CategoriaEvidencia.VISAO_GERAL);
        assertThat(v.getImagens().getFirst().getProtocoloItem()).isEqualTo("SALA_VISAO_GERAL");

        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        verify(storageService).store(eq(file), fileName.capture());
        assertThat(fileName.getValue())
                .matches("10_[0-9a-f-]{36}\\.jpg")
                .doesNotContain("nome-do-cliente");
    }

    @Test
    void shouldDeleteStoredFileWhenEvidencePersistenceFails() {
        Vistoria vistoria = inspectionWithRoute("Sala");
        MockMultipartFile file = new MockMultipartFile(
                "file", "evidencia.png", MediaType.IMAGE_PNG_VALUE, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(evidenceFileValidator.validate(file))
                .thenReturn(new ValidatedEvidence(".png", MediaType.IMAGE_PNG));
        when(storageService.store(eq(file), anyString())).thenReturn("uploads/evidencia.png");
        when(vistoriaRepository.saveAndFlush(any()))
                .thenThrow(new IllegalStateException("Falha ao persistir evidência"));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                10L, cliente, new RegistrarEvidenciaCommand(11L, "VISAO_GERAL"), file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao persistir evidência");

        verify(storageService).delete("uploads/evidencia.png");
        assertThat(vistoria.getImagens()).isEmpty();
    }

    @Test
    void shouldRejectEnvironmentFromAnotherInspectionBeforeValidatingFile() {
        Vistoria vistoria = inspectionWithRoute("Sala");
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                10L, cliente, new RegistrarEvidenciaCommand(999L, "VISAO_GERAL"), file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("ambiente");

        verifyNoInteractions(evidenceFileValidator, storageService);
        assertThat(vistoria.getImagens()).isEmpty();
    }

    @Test
    void shouldRejectUnknownEvidenceCategoryBeforeValidatingFile() {
        Vistoria vistoria = inspectionWithRoute("Sala");
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                10L, cliente, new RegistrarEvidenciaCommand(11L, "PANORAMA"), file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("categoria");

        verifyNoInteractions(evidenceFileValidator, storageService);
        assertThat(vistoria.getImagens()).isEmpty();
    }

    @Test
    void shouldPreserveExistingEvidenceWhenNewFileIsInvalid() {
        Vistoria vistoria = inspectionWithRoute("Sala");
        ImagemVistoria existing = evidence("uploads/anterior.jpg");
        vistoria.getImagens().add(existing);
        MockMultipartFile invalid = new MockMultipartFile(
                "file", "fraude.png", MediaType.IMAGE_PNG_VALUE, "texto".getBytes());
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(evidenceFileValidator.validate(invalid))
                .thenThrow(new InvalidEvidenceException("O conteúdo não corresponde ao tipo informado."));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                10L, cliente, new RegistrarEvidenciaCommand(11L, "DETALHE"), invalid))
                .isInstanceOf(InvalidEvidenceException.class);

        assertThat(vistoria.getImagens()).containsExactly(existing);
        verifyNoInteractions(storageService);
        verify(vistoriaRepository, never()).save(any());
    }

    @Test
    void shouldThrowVistoriaNotFoundWhenInspectionDoesNotExist() {
        when(vistoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                999L, cliente, new RegistrarEvidenciaCommand(11L, "VISAO_GERAL"), null))
                .isInstanceOf(VistoriaNotFoundException.class);
    }

    @Test
    void shouldThrowVistoriaAccessDeniedWhenInspectionBelongsToAnotherClient() {
        Vistoria vistoria = editableInspection();
        Usuario outroCliente = new Usuario("Outro", "outro2@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(outroCliente, "id", 55L);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(
                10L, outroCliente, new RegistrarEvidenciaCommand(11L, "VISAO_GERAL"), null))
                .isInstanceOf(VistoriaAccessDeniedException.class);
    }

    @Test
    void shouldThrowVistoriaNotFoundWhenApprovingUnknownInspection() {
        when(vistoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vistoriaService.aprovarVistoria(999L, engenheiro, "Parecer"))
                .isInstanceOf(VistoriaNotFoundException.class);
    }

    @Test
    void shouldRejectSubmissionWithoutPersistedEvidence() {
        Vistoria vistoria = editableInspection();
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.submeterVistoria(10L, cliente))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("ao menos uma evidência");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldReportEveryEnvironmentMissingAnOverviewBeforeSubmission() {
        Vistoria vistoria = inspectionWithRoute("Sala", "Quarto");
        addStructuredEvidence(vistoria, vistoria.getAmbientes().getFirst(), CategoriaEvidencia.VISAO_GERAL);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.submeterVistoria(10L, cliente))
                .isInstanceOfSatisfying(IncompleteInspectionException.class, exception ->
                        assertThat(exception.getAmbientesAusentes()).containsExactly("Quarto"));

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.EM_RASCUNHO);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldNotTreatDetailEvidenceAsEnvironmentOverview() {
        Vistoria vistoria = inspectionWithRoute("Sala");
        addStructuredEvidence(vistoria, vistoria.getAmbientes().getFirst(), CategoriaEvidencia.DETALHE);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.submeterVistoria(10L, cliente))
                .isInstanceOfSatisfying(IncompleteInspectionException.class, exception ->
                        assertThat(exception.getAmbientesAusentes()).containsExactly("Sala"));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldSubmitRoutedInspectionWhenEveryEnvironmentHasOverview() {
        Vistoria vistoria = inspectionWithRoute("Sala", "Quarto");
        vistoria.getAmbientes().forEach(ambiente ->
                addStructuredEvidence(vistoria, ambiente, CategoriaEvidencia.VISAO_GERAL));
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria)).thenReturn(vistoria);

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertThat(submetida.getStatus()).isEqualTo(VistoriaStatus.AGUARDANDO_IA);
        verify(eventPublisher).publishEvent(new VistoriaSubmetidaEvent(10L));
    }

    @Test
    void shouldSubmitInspectionAndPublishAnalysisEvent() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        
        v.getImagens().add(evidence("uploads/a.jpg"));
        v.getImagens().add(evidence("uploads/b.webp"));

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertThat(submetida).isSameAs(v);
        assertThat(submetida.getStatus()).isEqualTo(VistoriaStatus.AGUARDANDO_IA);
        assertThat(submetida.getPreLaudoIa()).isNull();
        assertThat(submetida.getDataConclusao()).isNull();
        ArgumentCaptor<VistoriaSubmetidaEvent> event = ArgumentCaptor.forClass(VistoriaSubmetidaEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().vistoriaId()).isEqualTo(10L);
    }

    @Test
    void shouldReturnCurrentStateWithoutPublishingDuplicateEventWhileAnalysisIsPending() {
        Vistoria v = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_IA);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertThat(submetida).isSameAs(v);
        assertThat(submetida.getStatus()).isEqualTo(VistoriaStatus.AGUARDANDO_IA);
        verify(vistoriaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldUpsertReviewWithoutChangingOriginalAnalysis() {
        Vistoria vistoria = reviewableInspection(validAnalysisWithTwoFindings());
        String originalAnalysis = vistoria.getPreLaudoIa();
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria)).thenReturn(vistoria);

        vistoriaService.revisarAchado(10L, cliente, new RevisarAchadoCommand(
                20L, 0, DecisaoRevisao.CONFIRMADO, "  Marca já observada.  ", null));
        Vistoria updated = vistoriaService.revisarAchado(10L, cliente, new RevisarAchadoCommand(
                20L, 0, DecisaoRevisao.CORRIGIDO, " É apenas uma sombra. ", " Sombra "));

        assertThat(updated.getPreLaudoIa()).isEqualTo(originalAnalysis);
        var reviews = new RevisaoAchadoStore(JsonMapper.builder().findAndAddModules().build())
                .read(updated.getRevisaoUsuario());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.getFirst().imagemId()).isEqualTo(20L);
        assertThat(reviews.getFirst().indiceAchado()).isZero();
        assertThat(reviews.getFirst().decisao()).isEqualTo(DecisaoRevisao.CORRIGIDO);
        assertThat(reviews.getFirst().contexto()).isEqualTo("É apenas uma sombra.");
        assertThat(reviews.getFirst().tipoCorrigido()).isEqualTo("Sombra");
        assertThat(reviews.getFirst().revisadoEm()).isEqualTo(Instant.parse("2026-09-30T12:00:00Z"));
    }

    @Test
    void shouldRejectReviewFromAnotherClient() {
        Usuario outroCliente = new Usuario("Outro", "outro-review@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(outroCliente, "id", 99L);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(
                reviewableInspection(validAnalysisWithTwoFindings())));

        assertThatThrownBy(() -> vistoriaService.revisarAchado(10L, outroCliente,
                new RevisarAchadoCommand(20L, 0, DecisaoRevisao.CONFIRMADO, "Confirmo.", null)))
                .isInstanceOf(VistoriaAccessDeniedException.class);
    }

    @Test
    void shouldRejectReviewForUnknownFinding() {
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(
                reviewableInspection(validAnalysisWithTwoFindings())));

        assertThatThrownBy(() -> vistoriaService.revisarAchado(10L, cliente,
                new RevisarAchadoCommand(20L, 99, DecisaoRevisao.CONFIRMADO, "Confirmo.", null)))
                .isInstanceOf(FindingNotFoundException.class);
    }

    @Test
    void shouldRequireCorrectedTypeForCorrectedFinding() {
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(
                reviewableInspection(validAnalysisWithTwoFindings())));

        assertThatThrownBy(() -> vistoriaService.revisarAchado(10L, cliente,
                new RevisarAchadoCommand(20L, 0, DecisaoRevisao.CORRIGIDO, "Corrijo.", "  ")))
                .isInstanceOf(InvalidReviewException.class)
                .hasMessage("Informe o tipo corrigido do achado.");
    }

    @Test
    void shouldRejectReviewOutsidePendingReviewState() {
        Vistoria vistoria = reviewableInspection(validAnalysisWithTwoFindings());
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.revisarAchado(10L, cliente,
                new RevisarAchadoCommand(20L, 0, DecisaoRevisao.CONFIRMADO, "Confirmo.", null)))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldBlockReportWhileAnyFindingHasNoReview() {
        Vistoria vistoria = reviewableInspection(validAnalysisWithTwoFindings());
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria)).thenReturn(vistoria);
        vistoriaService.revisarAchado(10L, cliente, new RevisarAchadoCommand(
                20L, 0, DecisaoRevisao.CONFIRMADO, "Confirmo.", null));

        assertThatThrownBy(() -> vistoriaService.concluirRelatorio(10L, cliente))
                .isInstanceOf(IncompleteReviewException.class)
                .hasMessage("Revise todos os achados antes de gerar o relatório.");
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.REVISAO_PENDENTE);
    }

    @Test
    void shouldMakeReportAvailableWhenEveryFindingIsReviewed() {
        Vistoria vistoria = reviewableInspection(validAnalysisWithTwoFindings());
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria)).thenReturn(vistoria);
        vistoriaService.revisarAchado(10L, cliente, new RevisarAchadoCommand(
                20L, 0, DecisaoRevisao.CONFIRMADO, "Confirmo.", null));
        vistoriaService.revisarAchado(10L, cliente, new RevisarAchadoCommand(
                20L, 1, DecisaoRevisao.REJEITADO, "Não corresponde ao local.", null));

        Vistoria completed = vistoriaService.concluirRelatorio(10L, cliente);

        assertThat(completed.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
        assertThat(completed.getDataConclusao()).isEqualTo(
                java.time.LocalDateTime.of(2026, 9, 30, 12, 0));
        assertThat(completed.getPreLaudoIa()).isEqualTo(validAnalysisWithTwoFindings());
    }

    @Test
    void shouldReturnSameReportWithoutSavingAgain() {
        Vistoria vistoria = reviewableInspection(validAnalysisWithTwoFindings());
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setDataConclusao(java.time.LocalDateTime.of(2026, 9, 30, 11, 0));
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        Vistoria result = vistoriaService.concluirRelatorio(10L, cliente);

        assertThat(result).isSameAs(vistoria);
        assertThat(result.getDataConclusao()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 30, 11, 0));
        verify(vistoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldAllowReportWhenValidAnalysisHasNoFindings() {
        Vistoria vistoria = reviewableInspection("""
                {"version":1,"images":[{
                  "storagePath":"uploads/a.jpg",
                  "imageQuality":{"usable":true,"issues":[]},
                  "limitations":[],"areas":[]
                }]}
                """);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria)).thenReturn(vistoria);

        Vistoria result = vistoriaService.concluirRelatorio(10L, cliente);

        assertThat(result.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
        assertThat(result.getDataConclusao()).isEqualTo(
                java.time.LocalDateTime.of(2026, 9, 30, 12, 0));
    }

    @Test
    void shouldAprovarVistoria() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria aprovada = vistoriaService.aprovarVistoria(10L, engenheiro, "Tudo certo");

        assertEquals(VistoriaStatus.CONCLUIDA, aprovada.getStatus());
        assertEquals("Tudo certo", aprovada.getParecerEngenheiro());
        assertEquals(engenheiro, aprovada.getEngenheiro());
    }

    @Test
    void shouldRejectApprovalWhenInspectionWasAlreadyProcessed() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.CONCLUIDA);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.aprovarVistoria(10L, engenheiro, "Parecer"))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldRejectReturnWhenInspectionWasAlreadyProcessed() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.CONCLUIDA);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.devolverAoCliente(10L, engenheiro, "Complementar"))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldTranslateConcurrentApprovalIntoStaleInspection() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria))
                .thenThrow(new ObjectOptimisticLockingFailureException(Vistoria.class, 10L));

        assertThatThrownBy(() -> vistoriaService.aprovarVistoria(10L, engenheiro, "Parecer"))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldTranslateConcurrentReturnIntoStaleInspection() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(vistoriaRepository.saveAndFlush(vistoria))
                .thenThrow(new ObjectOptimisticLockingFailureException(Vistoria.class, 10L));

        assertThatThrownBy(() -> vistoriaService.devolverAoCliente(10L, engenheiro, "Complementar"))
                .isInstanceOf(StaleInspectionException.class);
    }

    @Test
    void shouldAllowOwnerToReadEvidence() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.CONCLUIDA);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(storageService.load("uploads/a.jpg")).thenReturn(storedJpeg());

        EvidenceContent content = vistoriaService.buscarEvidencia(10L, 20L, cliente);

        assertThat(content.mediaType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(content.length()).isEqualTo(3);
    }

    @Test
    void shouldAllowEngineerToReadPendingEvidence() {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(storageService.load("uploads/a.jpg")).thenReturn(storedJpeg());

        EvidenceContent content = vistoriaService.buscarEvidencia(10L, 20L, engenheiro);

        assertThat(content.resource()).isNotNull();
    }

    @Test
    void shouldDenyAnotherClientWithoutLoadingFile() {
        Usuario outroCliente = new Usuario("Outro", "outro@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(outroCliente, "id", 99L);
        when(vistoriaRepository.findById(10L))
                .thenReturn(Optional.of(inspectionWithEvidence(VistoriaStatus.EM_RASCUNHO)));

        assertThatThrownBy(() -> vistoriaService.buscarEvidencia(10L, 20L, outroCliente))
                .isInstanceOf(EvidenceAccessDeniedException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldHideEvidenceBelongingToAnotherInspection() {
        when(vistoriaRepository.findById(10L))
                .thenReturn(Optional.of(inspectionWithEvidence(VistoriaStatus.EM_RASCUNHO)));

        assertThatThrownBy(() -> vistoriaService.buscarEvidencia(10L, 999L, cliente))
                .isInstanceOf(EvidenceNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectEngineerWhenInspectionIsNoLongerPending() {
        when(vistoriaRepository.findById(10L))
                .thenReturn(Optional.of(inspectionWithEvidence(VistoriaStatus.CONCLUIDA)));

        assertThatThrownBy(() -> vistoriaService.buscarEvidencia(10L, 20L, engenheiro))
                .isInstanceOf(StaleInspectionException.class);

        verifyNoInteractions(storageService);
    }

    private Vistoria editableInspection() {
        Vistoria vistoria = new Vistoria();
        vistoria.setId(10L);
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        return vistoria;
    }

    private ImagemVistoria evidence(String url) {
        ImagemVistoria image = new ImagemVistoria();
        image.setUrl(url);
        return image;
    }

    private Vistoria inspectionWithEvidence(VistoriaStatus status) {
        Vistoria vistoria = editableInspection();
        vistoria.setStatus(status);
        ImagemVistoria image = evidence("uploads/a.jpg");
        ReflectionTestUtils.setField(image, "id", 20L);
        vistoria.getImagens().add(image);
        return vistoria;
    }

    private StoredFile storedJpeg() {
        return new StoredFile(new ByteArrayResource(new byte[] {1, 2, 3}), MediaType.IMAGE_JPEG, 3);
    }

    private Vistoria reviewableInspection(String analysis) {
        Vistoria vistoria = inspectionWithEvidence(VistoriaStatus.REVISAO_PENDENTE);
        vistoria.setPreLaudoIa(analysis);
        return vistoria;
    }

    private Vistoria inspectionWithRoute(String... environmentNames) {
        Vistoria vistoria = editableInspection();
        List<AmbienteVistoria> ambientes = java.util.stream.IntStream.range(0, environmentNames.length)
                .mapToObj(index -> AmbienteVistoria.criar(
                        index == 0 ? TipoAmbiente.SALA : TipoAmbiente.QUARTO,
                        environmentNames[index],
                        index))
                .toList();
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, ambientes);
        for (int index = 0; index < ambientes.size(); index++) {
            ReflectionTestUtils.setField(ambientes.get(index), "id", 11L + index);
        }
        return vistoria;
    }

    private void addStructuredEvidence(
            Vistoria vistoria,
            AmbienteVistoria ambiente,
            CategoriaEvidencia categoria) {
        vistoria.getImagens().add(new ImagemVistoria(
                vistoria,
                ambiente,
                categoria,
                "uploads/" + ambiente.getId() + "-" + categoria.name() + ".jpg",
                java.time.LocalDateTime.of(2026, 9, 30, 9, 0)));
    }

    private String validAnalysisWithTwoFindings() {
        return """
                {"version":1,"images":[{
                  "storagePath":"uploads/a.jpg",
                  "imageQuality":{"usable":true,"issues":[]},
                  "limitations":[],
                  "areas":[
                    {"issueType":"stain","description":"Marca escura."},
                    {"issueType":"crack","description":"Linha fina."}
                  ]
                }]}
                """;
    }
}
