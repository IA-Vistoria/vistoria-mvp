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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Optional;

import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

class VistoriaServiceTest {

    @Mock
    private VistoriaRepository vistoriaRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private IaIntegrationService iaIntegrationService;

    @Mock
    private EvidenceFileValidator evidenceFileValidator;

    @InjectMocks
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
    }

    @Test
    void shouldCreateVistoriaIfCliente() {
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria result = vistoriaService.criarVistoria(cliente, "Endereço Teste");
        
        assertNotNull(result);
        assertEquals(cliente, result.getCliente());
        assertEquals(VistoriaStatus.EM_RASCUNHO, result.getStatus());
        assertEquals("Endereço Teste", result.getEndereco());
    }

    @Test
    void shouldUploadImagem() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        MockMultipartFile file = new MockMultipartFile("file", "nome-do-cliente.jpg", "image/jpeg", "test data".getBytes());
        when(evidenceFileValidator.validate(file))
                .thenReturn(new ValidatedEvidence(".jpg", MediaType.IMAGE_JPEG));
        when(storageService.store(eq(file), anyString())).thenReturn("uploads/a.jpg");
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria result = vistoriaService.uploadImagem(10L, cliente, "SALA_PISO", file);

        assertThat(result).isSameAs(v);
        assertEquals(1, v.getImagens().size());
        assertThat(v.getImagens().getFirst().getUrl()).isEqualTo("uploads/a.jpg");
        assertThat(v.getImagens().getFirst().getProtocoloItem()).isEqualTo("SALA_PISO");

        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        verify(storageService).store(eq(file), fileName.capture());
        assertThat(fileName.getValue())
                .matches("10_[0-9a-f-]{36}\\.jpg")
                .doesNotContain("nome-do-cliente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SALA", "TELHADO", ""})
    void shouldRejectUnknownProtocolItem(String protocoloItem) {
        Vistoria vistoria = editableInspection();
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(10L, cliente, protocoloItem, file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("protocolo");

        verifyNoInteractions(evidenceFileValidator, storageService);
        assertThat(vistoria.getImagens()).isEmpty();
    }

    @Test
    void shouldPreserveExistingEvidenceWhenNewFileIsInvalid() {
        Vistoria vistoria = editableInspection();
        ImagemVistoria existing = evidence("uploads/anterior.jpg");
        vistoria.getImagens().add(existing);
        MockMultipartFile invalid = new MockMultipartFile(
                "file", "fraude.png", MediaType.IMAGE_PNG_VALUE, "texto".getBytes());
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(evidenceFileValidator.validate(invalid))
                .thenThrow(new InvalidEvidenceException("O conteúdo não corresponde ao tipo informado."));

        assertThatThrownBy(() -> vistoriaService.uploadImagem(10L, cliente, "SALA_PISO", invalid))
                .isInstanceOf(InvalidEvidenceException.class);

        assertThat(vistoria.getImagens()).containsExactly(existing);
        verifyNoInteractions(storageService);
        verify(vistoriaRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubmissionWithoutPersistedEvidence() {
        Vistoria vistoria = editableInspection();
        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> vistoriaService.submeterVistoria(10L, cliente))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("ao menos uma evidência");

        verify(iaIntegrationService, never()).analisarImagens(anyList());
    }

    @Test
    void shouldSubmeterVistoriaAndGeneratePreLaudo() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        
        v.getImagens().add(evidence("uploads/a.jpg"));
        v.getImagens().add(evidence("uploads/b.webp"));

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(iaIntegrationService.analisarImagens(any())).thenReturn("Laudo Mock");

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertEquals(VistoriaStatus.AGUARDANDO_ENGENHEIRO, submetida.getStatus());
        assertEquals("Laudo Mock", submetida.getPreLaudoIa());
        verify(iaIntegrationService).analisarImagens(List.of("uploads/a.jpg", "uploads/b.webp"));
    }

    @Test
    void shouldSetFalhaIaIfIaFails() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        v.getImagens().add(evidence("uploads/a.jpg"));

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(iaIntegrationService.analisarImagens(any())).thenThrow(new RuntimeException("API error"));

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertEquals(VistoriaStatus.FALHA_IA, submetida.getStatus());
    }

    @Test
    void shouldAprovarVistoria() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Vistoria aprovada = vistoriaService.aprovarVistoria(10L, engenheiro, "Tudo certo");

        assertEquals(VistoriaStatus.CONCLUIDA, aprovada.getStatus());
        assertEquals("Tudo certo", aprovada.getParecerEngenheiro());
        assertEquals(engenheiro, aprovada.getEngenheiro());
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
}
