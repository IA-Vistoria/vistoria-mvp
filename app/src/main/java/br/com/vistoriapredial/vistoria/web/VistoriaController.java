package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.application.VistoriaService;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vistorias")
public class VistoriaController {

    private final VistoriaService vistoriaService;
    private final UsuarioRepository usuarioRepository;

    public VistoriaController(VistoriaService vistoriaService, UsuarioRepository usuarioRepository) {
        this.vistoriaService = vistoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario getUsuario(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado"));
    }

    // --- Fluxo do Cliente ---

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<VistoriaResponseDto> criarVistoria(
            @RequestBody @Valid CriarVistoriaRequestDto request,
            Authentication auth) {
        Usuario cliente = getUsuario(auth);
        Vistoria v = vistoriaService.criarVistoria(cliente, request.endereco());
        return ResponseEntity.status(HttpStatus.CREATED).body(VistoriaResponseDto.from(v));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<VistoriaResponseDto>> listarMinhas(Authentication auth) {
        Usuario cliente = getUsuario(auth);
        List<Vistoria> minhas = vistoriaService.listarVistoriasCliente(cliente);
        return ResponseEntity.ok(minhas.stream()
                .map(VistoriaResponseDto::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/imagens")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<VistoriaResponseDto> uploadImagem(
            @PathVariable Long id,
            @RequestParam("protocoloItem") String protocoloItem,
            @RequestParam("file") MultipartFile file,
        Authentication auth) {
        Usuario cliente = getUsuario(auth);
        Vistoria vistoria = vistoriaService.uploadImagem(id, cliente, protocoloItem, file);
        return ResponseEntity.ok(VistoriaResponseDto.from(vistoria));
    }

    @PostMapping("/{id}/submeter")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<VistoriaResponseDto> submeterVistoria(
            @PathVariable Long id,
            Authentication auth) {
        Usuario cliente = getUsuario(auth);
        Vistoria v = vistoriaService.submeterVistoria(id, cliente);
        return ResponseEntity.ok(VistoriaResponseDto.from(v));
    }

    // --- Fluxo do Engenheiro ---

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('ENGENHEIRO')")
    public ResponseEntity<List<VistoriaResponseDto>> listarPendentes(Authentication auth) {
        Usuario engenheiro = getUsuario(auth);
        List<Vistoria> pendentes = vistoriaService.listarPendentesEngenharia(engenheiro);
        return ResponseEntity.ok(pendentes.stream()
                .map(VistoriaResponseDto::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/analisar")
    @PreAuthorize("hasRole('ENGENHEIRO')")
    public ResponseEntity<VistoriaResponseDto> analisarVistoria(
            @PathVariable Long id,
            @RequestBody @Valid AprovarVistoriaRequestDto request,
            Authentication auth) {
        Usuario engenheiro = getUsuario(auth);
        
        Vistoria v;
        if (request.aprovado()) {
            v = vistoriaService.aprovarVistoria(id, engenheiro, request.parecer());
        } else {
            v = vistoriaService.devolverAoCliente(id, engenheiro, request.parecer());
        }
        
        return ResponseEntity.ok(VistoriaResponseDto.from(v));
    }
}
