package br.com.vistoriapredial.usuario.web;

import br.com.vistoriapredial.config.security.CustomAuthenticationEntryPoint;
import br.com.vistoriapredial.config.security.JwtAuthFilter;
import br.com.vistoriapredial.config.security.JwtService;
import br.com.vistoriapredial.config.security.SecurityConfig;
import br.com.vistoriapredial.usuario.application.UsuarioService;
import br.com.vistoriapredial.usuario.application.dto.AuthResponseDto;
import br.com.vistoriapredial.usuario.application.dto.LoginRequestDto;
import br.com.vistoriapredial.usuario.application.dto.RegisterRequestDto;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.shared.web.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, CustomAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Deve retornar 201 ao registrar usuário válido")
    void registerValid() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto("Nome", "email@ex.com", "senha123", PerfilEnum.ROLE_CLIENTE, null);
        AuthResponseDto res = new AuthResponseDto("token", 1L, "Nome", "ROLE_CLIENTE");

        when(usuarioService.register(any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"));
    }

    @Test
    @DisplayName("Deve retornar 422 se os inputs forem inválidos")
    void registerInvalid() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto("", "invalido", "12", null, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Deve retornar 401 se credenciais forem inválidas")
    void loginInvalid() throws Exception {
        LoginRequestDto req = new LoginRequestDto("email@ex.com", "errado");

        when(usuarioService.login(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
