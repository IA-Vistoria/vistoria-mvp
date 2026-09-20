package br.com.vistoriapredial.usuario.web;

import br.com.vistoriapredial.usuario.application.UsuarioService;
import br.com.vistoriapredial.usuario.application.dto.AuthResponseDto;
import br.com.vistoriapredial.usuario.application.dto.LoginRequestDto;
import br.com.vistoriapredial.usuario.application.dto.RegisterRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody @Valid RegisterRequestDto request) {
        AuthResponseDto response = usuarioService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
        AuthResponseDto response = usuarioService.login(request);
        return ResponseEntity.ok(response);
    }
}
