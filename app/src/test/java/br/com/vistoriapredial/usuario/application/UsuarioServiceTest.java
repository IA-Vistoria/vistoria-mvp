package br.com.vistoriapredial.usuario.application;

import br.com.vistoriapredial.config.security.JwtService;
import br.com.vistoriapredial.usuario.application.dto.RegisterRequestDto;
import br.com.vistoriapredial.usuario.application.dto.AuthResponseDto;
import br.com.vistoriapredial.usuario.application.exception.UsuarioConflictException;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(
                usuarioRepository,
                passwordEncoder,
                jwtService,
                "convite-seguro");
    }

    @Test
    @DisplayName("ROLE_CLIENTE anula o CREA")
    void clienteAnulaCrea() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Nome", "email@ex.com", "123", PerfilEnum.ROLE_CLIENTE, "CREA123", null);
        
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        
        Usuario mockSaved = new Usuario("Nome", "email@ex.com", "encoded", PerfilEnum.ROLE_CLIENTE, null);
        when(usuarioRepository.save(any())).thenReturn(mockSaved);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");

        AuthResponseDto response = usuarioService.register(request);

        assertNotNull(response);
        verify(usuarioRepository).save(argThat(usuario -> usuario.getCrea() == null));
    }

    @Test
    @DisplayName("ROLE_ENGENHEIRO sem CREA lança erro")
    void engenheiroSemCreaLancaErro() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Nome", "email@ex.com", "123", PerfilEnum.ROLE_ENGENHEIRO, null, "convite-seguro");
        
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> usuarioService.register(request));
        assertEquals("Engenheiro deve informar o CREA", ex.getMessage());
    }

    @Test
    @DisplayName("Email duplicado lança UsuarioConflictException")
    void emailDuplicadoLancaConflito() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Nome", "email@ex.com", "123", PerfilEnum.ROLE_CLIENTE, null, null);
        
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.of(mock(Usuario.class)));

        assertThrows(UsuarioConflictException.class, () -> usuarioService.register(request));
    }

    @Test
    @DisplayName("ROLE_ENGENHEIRO exige código de convite válido")
    void engenheiroExigeCodigoDeConviteValido() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Nome", "email@ex.com", "123456", PerfilEnum.ROLE_ENGENHEIRO,
                "CREA123", "codigo-invalido");

        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(
                br.com.vistoriapredial.usuario.application.exception.EngineerRegistrationDeniedException.class,
                () -> usuarioService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("ROLE_ENGENHEIRO aceita convite configurado")
    void engenheiroAceitaConviteConfigurado() {
        RegisterRequestDto request = new RegisterRequestDto(
                "Nome", "email@ex.com", "123456", PerfilEnum.ROLE_ENGENHEIRO,
                "CREA123", "convite-seguro");
        when(usuarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        Usuario saved = new Usuario(
                "Nome", "email@ex.com", "encoded", PerfilEnum.ROLE_ENGENHEIRO, "CREA123");
        when(usuarioRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("token");

        assertNotNull(usuarioService.register(request));
        verify(usuarioRepository).save(any());
    }
}
