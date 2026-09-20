package br.com.vistoriapredial.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JwtService.class);

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "segredo-exclusivo-para-testes-com-mais-de-32-bytes",
                86400000L);
    }

    @Test
    @DisplayName("Deve gerar um token válido e extrair username e claims")
    void deveGerarEValidarToken() {
        String token = jwtService.generateToken("joao@example.com", 1L, "ROLE_CLIENTE");

        assertNotNull(token);
        
        String username = jwtService.extractUsername(token);
        assertEquals("joao@example.com", username);

        Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));
        assertEquals(1L, userId);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("ROLE_CLIENTE", role);

        assertTrue(jwtService.isTokenValid(token, "joao@example.com"));
    }

    @Test
    @DisplayName("Não deve validar token para usuário incorreto")
    void naoDeveValidarParaOutroUsuario() {
        String token = jwtService.generateToken("joao@example.com", 1L, "ROLE_CLIENTE");
        assertFalse(jwtService.isTokenValid(token, "maria@example.com"));
    }

    @Test
    @DisplayName("Deve falhar ao iniciar sem segredo JWT")
    void deveFalharSemSegredoJwt() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Deve rejeitar segredo JWT menor que 32 bytes")
    void deveRejeitarSegredoCurto() {
        contextRunner
                .withPropertyValues("jwt.secret=curto", "jwt.expiration-ms=1234")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Deve usar a propriedade jwt.expiration-ms")
    void deveUsarPropriedadeDeExpiracaoDocumentada() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=segredo-de-teste-com-mais-de-32-bytes",
                        "jwt.expiration-ms=1234")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(ReflectionTestUtils.getField(
                            context.getBean(JwtService.class), "jwtExpiration"))
                            .isEqualTo(1234L);
                });
    }
}
