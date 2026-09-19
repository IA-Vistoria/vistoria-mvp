package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.config.security.JwtService;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.application.VistoriaService;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        when(vistoriaService.criarVistoria(any())).thenReturn(v);

        mockMvc.perform(post("/api/vistorias"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENTE")
    void shouldUploadImagem() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/api/vistorias/10/imagens")
                        .file(file)
                        .param("protocoloItem", "SALA"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "eng@test.com", roles = "ENGENHEIRO")
    void shouldListPendentes() throws Exception {
        Vistoria v = new Vistoria();
        v.setCliente(cliente);
        v.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        ReflectionTestUtils.setField(v, "id", 10L);

        when(vistoriaService.listarPendentesEngenharia(any())).thenReturn(List.of(v));

        mockMvc.perform(get("/api/vistorias/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
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
}
