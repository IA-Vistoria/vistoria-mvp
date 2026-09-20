package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.application.VistoriaService;
import br.com.vistoriapredial.vistoria.application.EvidenceContent;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vistorias")
public class VistoriaController {

    private static final Sort ORDENACAO_VISTORIAS = Sort.by(
            Sort.Order.desc("dataCriacao"),
            Sort.Order.desc("id"));

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
    public ResponseEntity<PaginaResponseDto<VistoriaResponseDto>> listarMinhas(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable) {
        Usuario cliente = getUsuario(auth);
        Page<Vistoria> minhas = vistoriaService.listarVistoriasCliente(
                cliente, paginacaoDeterministica(pageable));
        return ResponseEntity.ok(PaginaResponseDto.from(minhas, VistoriaResponseDto::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ENGENHEIRO')")
    public ResponseEntity<VistoriaResponseDto> buscarVistoria(
            @PathVariable Long id,
            Authentication auth) {
        Usuario usuario = getUsuario(auth);
        Vistoria vistoria = vistoriaService.buscarVistoria(id, usuario);
        return ResponseEntity.ok(VistoriaResponseDto.from(vistoria));
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

    @GetMapping("/{vistoriaId}/imagens/{imagemId}/conteudo")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ENGENHEIRO')")
    public ResponseEntity<Resource> buscarEvidencia(
            @PathVariable Long vistoriaId,
            @PathVariable Long imagemId,
            Authentication auth) {
        Usuario usuario = getUsuario(auth);
        EvidenceContent content = vistoriaService.buscarEvidencia(vistoriaId, imagemId, usuario);
        return ResponseEntity.ok()
                .contentType(content.mediaType())
                .contentLength(content.length())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(content.resource());
    }

    // --- Fluxo do Engenheiro ---

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('ENGENHEIRO')")
    public ResponseEntity<PaginaResponseDto<VistoriaResponseDto>> listarPendentes(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable) {
        Usuario engenheiro = getUsuario(auth);
        Page<Vistoria> pendentes = vistoriaService.listarPendentesEngenharia(
                engenheiro, paginacaoDeterministica(pageable));
        return ResponseEntity.ok(PaginaResponseDto.from(pendentes, VistoriaResponseDto::from));
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

    private Pageable paginacaoDeterministica(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDENACAO_VISTORIAS);
    }
}
