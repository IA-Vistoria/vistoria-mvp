package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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

        Vistoria v = vistoriaService.criarVistoria(cliente);
        
        assertNotNull(v);
        assertEquals(cliente, v.getCliente());
        assertEquals(VistoriaStatus.EM_RASCUNHO, v.getStatus());
    }

    @Test
    void shouldUploadImagem() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(storageService.store(any(), anyString())).thenReturn("http://local/img.jpg");
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
        ImagemVistoria img = vistoriaService.uploadImagem(10L, cliente, "SALA", file);

        assertNotNull(img);
        assertEquals("http://local/img.jpg", img.getUrl());
        assertEquals(1, v.getImagens().size());
    }

    @Test
    void shouldSubmeterVistoriaAndGeneratePreLaudo() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        
        ImagemVistoria img = new ImagemVistoria();
        img.setUrl("img.jpg");
        v.getImagens().add(img);

        when(vistoriaRepository.findById(10L)).thenReturn(Optional.of(v));
        when(vistoriaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(iaIntegrationService.analisarImagens(any())).thenReturn("Laudo Mock");

        Vistoria submetida = vistoriaService.submeterVistoria(10L, cliente);

        assertEquals(VistoriaStatus.AGUARDANDO_ENGENHEIRO, submetida.getStatus());
        assertEquals("Laudo Mock", submetida.getPreLaudoIa());
    }

    @Test
    void shouldSetFalhaIaIfIaFails() {
        Vistoria v = new Vistoria();
        v.setId(10L);
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        v.getImagens().add(new ImagemVistoria());

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
}
