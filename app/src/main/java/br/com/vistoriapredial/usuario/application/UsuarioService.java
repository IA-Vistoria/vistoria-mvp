package br.com.vistoriapredial.usuario.application;

import br.com.vistoriapredial.config.security.JwtService;
import br.com.vistoriapredial.usuario.application.dto.AuthResponseDto;
import br.com.vistoriapredial.usuario.application.dto.LoginRequestDto;
import br.com.vistoriapredial.usuario.application.dto.RegisterRequestDto;
import br.com.vistoriapredial.usuario.application.exception.UsuarioConflictException;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new UsuarioConflictException("Email já cadastrado");
        }

        String crea = request.crea();
        if (request.perfil() == PerfilEnum.ROLE_CLIENTE) {
            crea = null;
        } else if (request.perfil() == PerfilEnum.ROLE_ENGENHEIRO && (crea == null || crea.isBlank())) {
            throw new IllegalArgumentException("Engenheiro deve informar o CREA");
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.perfil(),
                crea
        );

        usuario = usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getId(), usuario.getPerfil().name());

        return new AuthResponseDto(token, usuario.getId(), usuario.getNome(), usuario.getPerfil().name());
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getId(), usuario.getPerfil().name());

        return new AuthResponseDto(token, usuario.getId(), usuario.getNome(), usuario.getPerfil().name());
    }
}
