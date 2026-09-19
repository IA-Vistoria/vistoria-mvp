package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.config.security.JwtService;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.application.VistoriaService;
import br.com.vistoriapredial.vistoria.application.EvidenceContent;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;
import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaNotFoundException;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VistoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VistoriaService vistoriaService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private Usuario cliente;
    private Usuario engenheiro;

    @BeforeEach
    void setUp() {
        cliente = new Usuario("Cliente", "client@test.com", "pass", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(cliente, "id", 1L);

        engenheiro = new Usuario("Eng", "eng@test.com", "pass", PerfilEnum.ROLE_ENGENHEIRO, "1234");
        ReflectionTestUtils.setField(engenheiro, "id", 2L);

        when(usuarioRepository.findByEmail("client@test.com")).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("eng@test.com")).thenReturn(Optional.of(engenheiro));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldCreateVistoria() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        ReflectionTestUtils.setField(v, "id", 10L);

        when(vistoriaService.criarVistoria(any(), any())).thenReturn(v);

        String payload = """
                {
                    "endereco": "Rua 1"
                }
                """;

        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectAddressLargerThanPersistenceLimit() throws Exception {
        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endereco\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldReturnProblemDetailWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/vistorias/minhas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:unauthorized"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnProblemDetailWhenRoleHasNoPermission() throws Exception {
        mockMvc.perform(get("/api/vistorias/pendentes"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:forbidden"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldUploadImagem() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "img".getBytes());
        Vistoria updated = new Vistoria();
        updated.setCliente(cliente);
        updated.setStatus(VistoriaStatus.EM_RASCUNHO);
        ReflectionTestUtils.setField(updated, "id", 10L);
        ImagemVistoria image = new ImagemVistoria();
        image.setProtocoloItem("SALA_PISO");
        image.setDataUpload(LocalDateTime.of(2026, 9, 19, 4, 0));
        ReflectionTestUtils.setField(image, "id", 20L);
        updated.getImagens().add(image);
        when(vistoriaService.uploadImagem(eq(10L), any(), eq("SALA_PISO"), any())).thenReturn(updated);

        mockMvc.perform(multipart("/api/vistorias/10/imagens")
                        .file(file)
                        .param("protocoloItem", "SALA_PISO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.imagens[0].id").value(20))
                .andExpect(jsonPath("$.imagens[0].protocoloItem").value("SALA_PISO"))
                .andExpect(jsonPath("$.imagens[0].dataUpload").exists())
                .andExpect(jsonPath("$.imagens[0].conteudoUrl")
                        .value("/api/vistorias/10/imagens/20/conteudo"))
                .andExpect(jsonPath("$.imagens[0].url").doesNotExist());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnProblemDetailForInvalidEvidence() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "fraude.png", "image/png", "texto".getBytes());
        doThrow(new InvalidEvidenceException("O conteúdo não corresponde ao tipo informado."))
                .when(vistoriaService).uploadImagem(eq(10L), any(), eq("SALA_PISO"), any());

        mockMvc.perform(multipart("/api/vistorias/10/imagens")
                        .file(file)
                        .param("protocoloItem", "SALA_PISO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Evidência inválida"))
                .andExpect(jsonPath("$.detail").value("O conteúdo não corresponde ao tipo informado."))
                .andExpect(jsonPath("$.instance").value("/api/vistorias/10/imagens"));
    }

    @Test
    @WithMockUser(username = "eng@test.com", roles = "ENGENHEIRO")
    void shouldListPendentes() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        ReflectionTestUtils.setField(v, "id", 10L);

        when(vistoriaService.listarPendentesEngenharia(any(), any()))
                .thenReturn(new PageImpl<>(List.of(v), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/vistorias/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.pagina").value(0));
    }

    @Test
    @WithMockUser(username = "eng@test.com", roles = "ENGENHEIRO")
    void shouldFixarOrdenacaoDaFilaMesmoComSortExternoInvalido() throws Exception {
        when(vistoriaService.listarPendentesEngenharia(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 7), 0));

        mockMvc.perform(get("/api/vistorias/pendentes?page=2&size=7&sort=campoInexistente,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(vistoriaService).listarPendentesEngenharia(eq(engenheiro), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(7);
        assertThat(pageable.getSort()).containsExactly(
                Sort.Order.desc("dataCriacao"),
                Sort.Order.desc("id"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldListMinhasComPaginacao() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        ReflectionTestUtils.setField(v, "id", 11L);

        when(vistoriaService.listarVistoriasCliente(any(), any()))
                .thenReturn(new PageImpl<>(List.of(v), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/vistorias/minhas?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(5))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldFixarOrdenacaoDasMinhasMesmoComSortExternoInvalido() throws Exception {
        when(vistoriaService.listarVistoriasCliente(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 4), 0));

        mockMvc.perform(get("/api/vistorias/minhas?page=3&size=4&sort=campoInexistente,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(vistoriaService).listarVistoriasCliente(eq(cliente), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(4);
        assertThat(pageable.getSort()).containsExactly(
                Sort.Order.desc("dataCriacao"),
                Sort.Order.desc("id"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldBuscarVistoriaPorId() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.EM_RASCUNHO);
        ReflectionTestUtils.setField(v, "id", 10L);

        when(vistoriaService.buscarVistoria(eq(10L), any())).thenReturn(v);

        mockMvc.perform(get("/api/vistorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnNotFoundWhenBuscandoVistoriaInexistente() throws Exception {
        when(vistoriaService.buscarVistoria(eq(999L), any()))
                .thenThrow(new VistoriaNotFoundException());

        mockMvc.perform(get("/api/vistorias/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:vistoria-not-found"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnForbiddenWhenBuscandoVistoriaDeOutroCliente() throws Exception {
        when(vistoriaService.buscarVistoria(eq(10L), any()))
                .thenThrow(new VistoriaAccessDeniedException());

        mockMvc.perform(get("/api/vistorias/10"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:forbidden"));
    }

    @Test
    @WithMockUser(username = "eng@test.com", roles = "ENGENHEIRO")
    void shouldAnalisarVistoriaAprovada() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.CONCLUIDA);
        ReflectionTestUtils.setField(v, "id", 10L);

        when(vistoriaService.aprovarVistoria(eq(10L), any(), eq("Parecer ok"))).thenReturn(v);

        String payload = """
                {
                    "parecer": "Parecer ok",
                    "aprovado": true
                }
                """;

        mockMvc.perform(post("/api/vistorias/10/analisar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnAuthenticatedEvidenceContent() throws Exception {
        ByteArrayResource resource = new ByteArrayResource(new byte[] {1, 2, 3});
        when(vistoriaService.buscarEvidencia(10L, 20L, cliente))
                .thenReturn(new EvidenceContent(resource, MediaType.IMAGE_JPEG, 3));

        mockMvc.perform(get("/api/vistorias/10/imagens/20/conteudo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().longValue("Content-Length", 3))
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnForbiddenProblemForAnotherClient() throws Exception {
        when(vistoriaService.buscarEvidencia(10L, 20L, cliente))
                .thenThrow(new EvidenceAccessDeniedException());

        mockMvc.perform(get("/api/vistorias/10/imagens/20/conteudo"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:forbidden"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnNotFoundForMismatchedEvidencePair() throws Exception {
        when(vistoriaService.buscarEvidencia(10L, 999L, cliente))
                .thenThrow(new EvidenceNotFoundException());

        mockMvc.perform(get("/api/vistorias/10/imagens/999/conteudo"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:evidence-not-found"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnNotFoundWhenSubmittingUnknownInspection() throws Exception {
        when(vistoriaService.submeterVistoria(eq(999L), any()))
                .thenThrow(new VistoriaNotFoundException());

        mockMvc.perform(post("/api/vistorias/999/submeter"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:vistoria-not-found"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnForbiddenWhenInspectionBelongsToAnotherClient() throws Exception {
        when(vistoriaService.submeterVistoria(eq(10L), any()))
                .thenThrow(new VistoriaAccessDeniedException());

        mockMvc.perform(post("/api/vistorias/10/submeter"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:forbidden"));
    }

    @Test
    @WithMockUser(username = "eng@test.com", roles = "ENGENHEIRO")
    void shouldReturnConflictWhenEngineerCaseIsStale() throws Exception {
        when(vistoriaService.aprovarVistoria(eq(10L), any(), eq("Parecer")))
                .thenThrow(new StaleInspectionException());

        mockMvc.perform(post("/api/vistorias/10/analisar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parecer\":\"Parecer\",\"aprovado\":true}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:stale-inspection"))
                .andExpect(jsonPath("$.detail")
                        .value("A vistoria já foi processada ou alterada por outra sessão."));
    }

}
