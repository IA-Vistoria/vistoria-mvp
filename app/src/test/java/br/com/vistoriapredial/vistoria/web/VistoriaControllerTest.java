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
import br.com.vistoriapredial.vistoria.application.exception.IncompleteInspectionException;
import br.com.vistoriapredial.vistoria.application.command.RegistrarEvidenciaCommand;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaNotFoundException;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.RoteiroVistoriaConflitoException;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
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
import br.com.vistoriapredial.vistoria.application.exception.FindingNotFoundException;

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
        v.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0)));
        ReflectionTestUtils.setField(v, "id", 10L);
        ReflectionTestUtils.setField(v, "version", 0L);

        when(vistoriaService.criarVistoria(any(), any())).thenReturn(v);

        String payload = """
                {
                    "endereco": "Rua 1",
                    "tipoImovel": "APARTAMENTO",
                    "ambientes": [{"tipo":"SALA","nome":"Sala"}]
                }
                """;

        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.tipoImovel").value("APARTAMENTO"))
                .andExpect(jsonPath("$.ambientes[0].nome").value("Sala"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectAddressLargerThanPersistenceLimit() throws Exception {
        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endereco\":\"" + "a".repeat(256)
                                + "\",\"tipoImovel\":\"CASA\",\"ambientes\":[{\"tipo\":\"SALA\",\"nome\":\"Sala\"}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectCreationWithoutRooms() throws Exception {
        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endereco":"Rua 1","tipoImovel":"CASA","ambientes":[]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/ambientes"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldPointToInvalidRoomNameInsideRoute() throws Exception {
        mockMvc.perform(post("/api/vistorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endereco":"Rua 1","tipoImovel":"CASA","ambientes":[
                                  {"tipo":"SALA","nome":"A"}
                                ]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/ambientes/0/nome"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldUpdateInspectionRoute() throws Exception {
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        vistoria.configurarRoteiro(TipoImovel.CASA, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala integrada", 0)));
        ReflectionTestUtils.setField(vistoria, "id", 10L);
        ReflectionTestUtils.setField(vistoria, "version", 4L);
        when(vistoriaService.atualizarRoteiro(eq(10L), eq(cliente), any())).thenReturn(vistoria);

        mockMvc.perform(put("/api/vistorias/10/roteiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":3,"tipoImovel":"CASA","ambientes":[
                                  {"id":11,"tipo":"SALA","nome":"Sala integrada"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.ambientes[0].nome").value("Sala integrada"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnConflictWhenRouteWouldRemoveEvidence() throws Exception {
        when(vistoriaService.atualizarRoteiro(eq(10L), eq(cliente), any()))
                .thenThrow(new RoteiroVistoriaConflitoException(
                        "O ambiente Sala possui evidências e não pode ser removido."));

        mockMvc.perform(put("/api/vistorias/10/roteiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":3,"tipoImovel":"CASA","ambientes":[
                                  {"id":12,"tipo":"QUARTO","nome":"Quarto"}
                                ]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:route-conflict"));
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
        updated.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala de estar", 0)));
        ReflectionTestUtils.setField(updated, "id", 10L);
        AmbienteVistoria sala = updated.getAmbientes().getFirst();
        ReflectionTestUtils.setField(sala, "id", 11L);
        ImagemVistoria image = new ImagemVistoria(
                updated,
                sala,
                CategoriaEvidencia.VISAO_GERAL,
                "uploads/sala.jpg",
                LocalDateTime.of(2026, 9, 19, 4, 0));
        ReflectionTestUtils.setField(image, "id", 20L);
        updated.getImagens().add(image);
        when(vistoriaService.uploadImagem(eq(10L), any(), any(RegistrarEvidenciaCommand.class), any()))
                .thenReturn(updated);

        mockMvc.perform(multipart("/api/vistorias/10/imagens")
                        .file(file)
                        .param("ambienteId", "11")
                        .param("categoria", "VISAO_GERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.imagens[0].id").value(20))
                .andExpect(jsonPath("$.imagens[0].ambienteId").value(11))
                .andExpect(jsonPath("$.imagens[0].ambienteNome").value("Sala de estar"))
                .andExpect(jsonPath("$.imagens[0].categoria").value("VISAO_GERAL"))
                .andExpect(jsonPath("$.imagens[0].protocoloItem").value("SALA_VISAO_GERAL"))
                .andExpect(jsonPath("$.imagens[0].dataUpload").exists())
                .andExpect(jsonPath("$.imagens[0].conteudoUrl")
                        .value("/api/vistorias/10/imagens/20/conteudo"))
                .andExpect(jsonPath("$.imagens[0].storagePath").doesNotExist());

        ArgumentCaptor<RegistrarEvidenciaCommand> command =
                ArgumentCaptor.forClass(RegistrarEvidenciaCommand.class);
        verify(vistoriaService).uploadImagem(eq(10L), eq(cliente), command.capture(), eq(file));
        assertThat(command.getValue().ambienteId()).isEqualTo(11L);
        assertThat(command.getValue().categoria()).isEqualTo("VISAO_GERAL");
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldExposeTypedAiAnalysisWithoutRawPayloadOrStoragePath() throws Exception {
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.CONCLUIDA);
        vistoria.setPreLaudoIa("""
                {"version":1,"images":[{
                  "storagePath":"uploads/foto.jpg",
                  "analysisId":"ana-7",
                  "overallSummary":"Marca visual identificada.",
                  "limitations":["Sem medição."],
                  "imageQuality":{"usable":true,"issues":[]},
                  "areas":[{"issueType":"stain","description":"Marca escura."}]
                }]}
                """);
        ReflectionTestUtils.setField(vistoria, "id", 10L);
        ImagemVistoria image = new ImagemVistoria();
        image.setUrl("uploads/foto.jpg");
        image.setProtocoloItem("SALA_PAREDES_REVESTIMENTOS");
        ReflectionTestUtils.setField(image, "id", 20L);
        vistoria.getImagens().add(image);
        when(vistoriaService.buscarVistoria(10L, cliente)).thenReturn(vistoria);

        mockMvc.perform(get("/api/vistorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analiseIa.version").value(1))
                .andExpect(jsonPath("$.analiseIa.imagens[0].imagemId").value(20))
                .andExpect(jsonPath("$.analiseIa.imagens[0].achados[0].indice").value(0))
                .andExpect(jsonPath("$.analiseIa.imagens[0].achados[0].tipo").value("stain"))
                .andExpect(jsonPath("$.preLaudoIa").doesNotExist())
                .andExpect(jsonPath("$.imagens[0].storagePath").doesNotExist());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldExposeCompleteV2AnalysisSeparatedFromManifestations() throws Exception {
        Vistoria vistoria = analyzedV2InspectionResponse();
        vistoria.setRevisaoUsuario("""
                {"version":1,"revisoes":[{
                  "imagemId":31,"indiceAchado":0,"decisao":"CONTESTO",
                  "contexto":"A parede foi pintada ontem.","tipoCorrigido":null,
                  "revisadoEm":"2026-09-30T20:10:00Z"
                }]}
                """);
        when(vistoriaService.buscarVistoria(10L, cliente)).thenReturn(vistoria);

        mockMvc.perform(get("/api/vistorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELATORIO_DISPONIVEL"))
                .andExpect(jsonPath("$.analiseIa.version").value(2))
                .andExpect(jsonPath("$.analiseIa.execucao.provider").value("oci"))
                .andExpect(jsonPath("$.analiseIa.execucao.modelo").value("google.gemini-2.5-flash"))
                .andExpect(jsonPath("$.analiseIa.resultadoGeral").value("NAO_APROVADO"))
                .andExpect(jsonPath("$.analiseIa.motivoResultadoGeral").isNotEmpty())
                .andExpect(jsonPath("$.analiseIa.ambientes[0].id").value(8))
                .andExpect(jsonPath("$.analiseIa.ambientes[0].resultado").value("NAO_APROVADO"))
                .andExpect(jsonPath("$.analiseIa.imagens[0].imagemId").value(31))
                .andExpect(jsonPath("$.analiseIa.imagens[0].ambiente.nome").value("Banheiro"))
                .andExpect(jsonPath("$.analiseIa.imagens[0].achados[0].criterio")
                        .value("Integridade aparente da parede"))
                .andExpect(jsonPath("$.analiseIa.imagens[0].achados[0].impacto")
                        .value("Pode indicar umidade persistente."))
                .andExpect(jsonPath("$.manifestacoes[0].decisao").value("CONTESTO"))
                .andExpect(jsonPath("$.manifestacoes[0].contexto").value("A parede foi pintada ontem."))
                .andExpect(jsonPath("$.preLaudoIa").doesNotExist());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldExposeLegacyManifestationWithoutChangingV1Analysis() throws Exception {
        Vistoria vistoria = reviewableInspectionResponse();
        vistoria.setRevisaoUsuario("""
                {"version":1,"revisoes":[{
                  "imagemId":20,"indiceAchado":0,"decisao":"CONFIRMADO",
                  "contexto":"Registro anterior.","tipoCorrigido":null,
                  "revisadoEm":"2026-09-30T12:00:00Z"
                }]}
                """);
        when(vistoriaService.buscarVistoria(10L, cliente)).thenReturn(vistoria);

        mockMvc.perform(get("/api/vistorias/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analiseIa.version").value(1))
                .andExpect(jsonPath("$.manifestacoes[0].decisao").value("CONFIRMADO"))
                .andExpect(jsonPath("$.manifestacoes[0].contexto").value("Registro anterior."));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnProblemDetailForInvalidEvidence() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "fraude.png", "image/png", "texto".getBytes());
        doThrow(new InvalidEvidenceException("O conteúdo não corresponde ao tipo informado."))
                .when(vistoriaService).uploadImagem(eq(10L), any(), any(RegistrarEvidenciaCommand.class), any());

        mockMvc.perform(multipart("/api/vistorias/10/imagens")
                        .file(file)
                        .param("ambienteId", "11")
                        .param("categoria", "VISAO_GERAL"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Evidência inválida"))
                .andExpect(jsonPath("$.detail").value("O conteúdo não corresponde ao tipo informado."))
                .andExpect(jsonPath("$.instance").value("/api/vistorias/10/imagens"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnMissingEnvironmentsWhenSubmissionIsIncomplete() throws Exception {
        when(vistoriaService.submeterVistoria(10L, cliente))
                .thenThrow(new IncompleteInspectionException(List.of("Quarto", "Varanda")));

        mockMvc.perform(post("/api/vistorias/10/submeter"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:incomplete-inspection"))
                .andExpect(jsonPath("$.title").value("Vistoria incompleta"))
                .andExpect(jsonPath("$.ambientesAusentes[0]").value("Quarto"))
                .andExpect(jsonPath("$.ambientesAusentes[1]").value("Varanda"));
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
    void shouldListMinhasFiltrandoStatusNoServidor() throws Exception {
        Vistoria relatorio = new Vistoria();
        relatorio.setCliente(cliente);
        relatorio.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        ReflectionTestUtils.setField(relatorio, "id", 12L);
        when(vistoriaService.listarVistoriasClientePorStatus(
                any(), eq(VistoriaStatus.RELATORIO_DISPONIVEL), any()))
                .thenReturn(new PageImpl<>(List.of(relatorio), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/vistorias/minhas?status=RELATORIO_DISPONIVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[0].status").value("RELATORIO_DISPONIVEL"));

        verify(vistoriaService).listarVistoriasClientePorStatus(
                eq(cliente), eq(VistoriaStatus.RELATORIO_DISPONIVEL), any());
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
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldAcceptSubmissionBeforeAiProcessingFinishes() throws Exception {
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.AGUARDANDO_IA);
        ReflectionTestUtils.setField(vistoria, "id", 10L);
        when(vistoriaService.submeterVistoria(10L, cliente)).thenReturn(vistoria);

        mockMvc.perform(post("/api/vistorias/10/submeter"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_IA"))
                .andExpect(jsonPath("$.analiseIa").doesNotExist());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRegisterAgreementWithoutMandatoryText() throws Exception {
        Vistoria vistoria = reviewableInspectionResponse();
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setRevisaoUsuario("""
                {"version":1,"revisoes":[{
                  "imagemId":20,"indiceAchado":0,"decisao":"CONCORDO",
                  "contexto":null,"tipoCorrigido":null,
                  "revisadoEm":"2026-09-30T12:00:00Z"
                }]}
                """);
        when(vistoriaService.revisarAchado(eq(10L), eq(cliente), any())).thenReturn(vistoria);

        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0,"decisao":"CONCORDO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELATORIO_DISPONIVEL"))
                .andExpect(jsonPath("$.manifestacoes[0].imagemId").value(20))
                .andExpect(jsonPath("$.manifestacoes[0].indiceAchado").value(0))
                .andExpect(jsonPath("$.manifestacoes[0].decisao").value("CONCORDO"))
                .andExpect(jsonPath("$.manifestacoes[0].contexto").doesNotExist());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRegisterContestWithJustification() throws Exception {
        Vistoria vistoria = reviewableInspectionResponse();
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setRevisaoUsuario("""
                {"version":1,"revisoes":[{
                  "imagemId":20,"indiceAchado":0,"decisao":"CONTESTO",
                  "contexto":"A marca é uma sombra.","tipoCorrigido":null,
                  "revisadoEm":"2026-09-30T12:00:00Z"
                }]}
                """);
        when(vistoriaService.revisarAchado(eq(10L), eq(cliente), any())).thenReturn(vistoria);

        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0,"decisao":"CONTESTO",
                                 "contexto":"A marca é uma sombra."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifestacoes[0].decisao").value("CONTESTO"))
                .andExpect(jsonPath("$.manifestacoes[0].contexto").value("A marca é uma sombra."));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRegisterAdditionalContextSeparately() throws Exception {
        Vistoria vistoria = reviewableInspectionResponse();
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setRevisaoUsuario("""
                {"version":1,"revisoes":[{
                  "imagemId":20,"indiceAchado":0,"decisao":"CONTEXTO_ADICIONAL",
                  "contexto":"A parede foi reparada em agosto.","tipoCorrigido":null,
                  "revisadoEm":"2026-09-30T12:00:00Z"
                }]}
                """);
        when(vistoriaService.revisarAchado(eq(10L), eq(cliente), any())).thenReturn(vistoria);

        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0,"decisao":"CONTEXTO_ADICIONAL",
                                 "contexto":"A parede foi reparada em agosto."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifestacoes[0].decisao").value("CONTEXTO_ADICIONAL"))
                .andExpect(jsonPath("$.manifestacoes[0].contexto")
                        .value("A parede foi reparada em agosto."));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectNegativeFindingIndexAtHttpBoundary() throws Exception {
        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":-1,"decisao":"CONCORDO"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/indiceAchado"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectMissingManifestationTypeAtHttpBoundary() throws Exception {
        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/decisao"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldRejectManifestationContextAboveLimitAtHttpBoundary() throws Exception {
        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0,"decisao":"CONTESTO",
                                 "contexto":"%s"}
                                """.formatted("x".repeat(1001))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/contexto"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnNotFoundForUnknownFinding() throws Exception {
        when(vistoriaService.revisarAchado(eq(10L), eq(cliente), any()))
                .thenThrow(new FindingNotFoundException());

        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":99,"decisao":"CONCORDO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:finding-not-found"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldReturnForbiddenWhenAnotherClientTriesToManifest() throws Exception {
        when(vistoriaService.revisarAchado(eq(10L), eq(cliente), any()))
                .thenThrow(new VistoriaAccessDeniedException());

        mockMvc.perform(put("/api/vistorias/10/revisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imagemId":20,"indiceAchado":0,"decisao":"CONCORDO"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:vistoria:problem:forbidden"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldMakeReportAvailable() throws Exception {
        Vistoria vistoria = reviewableInspectionResponse();
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setDataConclusao(LocalDateTime.of(2026, 9, 30, 12, 0));
        when(vistoriaService.concluirRelatorio(10L, cliente)).thenReturn(vistoria);

        mockMvc.perform(post("/api/vistorias/10/relatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELATORIO_DISPONIVEL"))
                .andExpect(jsonPath("$.dataConclusao").value("2026-09-30T12:00:00"));
    }

    private Vistoria reviewableInspectionResponse() {
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.REVISAO_PENDENTE);
        vistoria.setPreLaudoIa("""
                {"version":1,"images":[{
                  "storagePath":"uploads/a.jpg",
                  "imageQuality":{"usable":true,"issues":[]},
                  "limitations":[],
                  "areas":[{"issueType":"stain","description":"Marca escura."}]
                }]}
                """);
        ReflectionTestUtils.setField(vistoria, "id", 10L);
        ImagemVistoria image = new ImagemVistoria();
        image.setId(20L);
        image.setUrl("uploads/a.jpg");
        image.setProtocoloItem("SALA_PAREDES_REVESTIMENTOS");
        vistoria.getImagens().add(image);
        return vistoria;
    }

    private Vistoria analyzedV2InspectionResponse() {
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(
                AmbienteVistoria.criar(TipoAmbiente.BANHEIRO, "Banheiro", 0)));
        AmbienteVistoria ambiente = vistoria.getAmbientes().getFirst();
        ReflectionTestUtils.setField(ambiente, "id", 8L);
        ImagemVistoria imagem = new ImagemVistoria(
                vistoria, ambiente, CategoriaEvidencia.VISAO_GERAL,
                "uploads/banheiro.webp", LocalDateTime.of(2026, 9, 30, 19, 0));
        imagem.setId(31L);
        vistoria.getImagens().add(imagem);
        vistoria.setPreLaudoIa("""
                {
                  "version":2,
                  "execution":{"provider":"oci","model":"google.gemini-2.5-flash",
                    "promptVersion":"vistoria-visual-v2","analysisId":"ana-7",
                    "completedAt":"2026-09-30T20:00:00Z"},
                  "images":[{"imageId":31,"storagePath":"uploads/banheiro.webp",
                    "environment":{"id":8,"name":"Banheiro","category":"VISAO_GERAL"},
                    "imageQuality":"SUFICIENTE","summary":"Mofo aparente.",
                    "limitations":["Sem medição de umidade."],"captureGuidance":null,
                    "findings":[{"criterion":"Integridade aparente da parede","area":"parede",
                      "type":"mofo_aparente","description":"Manchas escuras.",
                      "evidence":"Distribuição extensa.","impact":"Pode indicar umidade persistente.",
                      "severity":"ALTA","confidence":"ALTA","recommendation":"Avaliação presencial.",
                      "location":"parede ao lado da porta"}]}],
                  "environments":[{"id":8,"name":"Banheiro","result":"NAO_APROVADO",
                    "resultReason":"Há indício visual de alta gravidade."}],
                  "overallResult":"NAO_APROVADO",
                  "overallReason":"Há indício visual de alta gravidade."
                }
                """);
        ReflectionTestUtils.setField(vistoria, "id", 10L);
        return vistoria;
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
