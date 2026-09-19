package br.com.vistoriapredial.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
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
}
