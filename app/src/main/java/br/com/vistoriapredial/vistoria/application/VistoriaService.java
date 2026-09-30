package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.application.command.AtualizarRoteiroCommand;
import br.com.vistoriapredial.vistoria.application.command.CriarVistoriaCommand;
import br.com.vistoriapredial.vistoria.application.command.RegistrarEvidenciaCommand;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import br.com.vistoriapredial.vistoria.application.exception.IncompleteInspectionException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.VistoriaNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import br.com.vistoriapredial.vistoria.application.analysis.VistoriaSubmetidaEvent;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.application.analysis.PreLaudoParser;
import br.com.vistoriapredial.vistoria.application.exception.IncompleteReviewException;
import br.com.vistoriapredial.vistoria.application.exception.InvalidReviewException;
import br.com.vistoriapredial.vistoria.application.review.DecisaoRevisao;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchado;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchadoStore;
import br.com.vistoriapredial.vistoria.application.review.RevisarAchadoCommand;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final StorageService storageService;
    private final EvidenceFileValidator evidenceFileValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final PreLaudoParser preLaudoParser;
    private final RevisaoAchadoStore reviewStore;
    private final Clock clock;

    public VistoriaService(
            VistoriaRepository vistoriaRepository,
            StorageService storageService,
            EvidenceFileValidator evidenceFileValidator,
            ApplicationEventPublisher eventPublisher,
            PreLaudoParser preLaudoParser,
            RevisaoAchadoStore reviewStore,
            Clock clock) {
        this.vistoriaRepository = vistoriaRepository;
        this.storageService = storageService;
        this.evidenceFileValidator = evidenceFileValidator;
        this.eventPublisher = eventPublisher;
        this.preLaudoParser = preLaudoParser;
        this.reviewStore = reviewStore;
        this.clock = clock;
    }

    // Fluxo Cliente

    @Transactional
    public Vistoria criarVistoria(Usuario cliente, CriarVistoriaCommand command) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem criar vistorias");
        }
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setEndereco(command.endereco());
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        vistoria.configurarRoteiro(
                command.tipoImovel(),
                IntStream.range(0, command.ambientes().size())
                        .mapToObj(indice -> AmbienteVistoria.criar(
                                command.ambientes().get(indice).tipo(),
                                command.ambientes().get(indice).nome(),
                                indice))
                        .toList());
        return vistoriaRepository.save(vistoria);
    }

    @Transactional
    public Vistoria atualizarRoteiro(
            Long vistoriaId,
            Usuario cliente,
            AtualizarRoteiroCommand command) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        if (!Objects.equals(vistoria.getVersion(), command.version())) {
            throw new StaleInspectionException();
        }
        vistoria.atualizarRoteiro(
                command.tipoImovel(),
                command.ambientes().stream().map(item -> item.toDomain()).toList());
        return salvarComControleConcorrencia(vistoria);
    }

    @Transactional(readOnly = true)
    public Page<Vistoria> listarVistoriasCliente(Usuario cliente, Pageable pageable) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem listar suas vistorias");
        }
        return comImagensCarregadas(vistoriaRepository.findByCliente(cliente, pageable));
    }

    @Transactional
    public Vistoria uploadImagem(
            Long vistoriaId,
            Usuario cliente,
            RegistrarEvidenciaCommand command,
            MultipartFile file) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE) {
            throw new StaleInspectionException();
        }

        AmbienteVistoria ambiente = validarAmbiente(vistoria, command);
        CategoriaEvidencia categoria = validarCategoria(command);

        ValidatedEvidence validated = evidenceFileValidator.validate(file);
        String fileName = vistoriaId + "_" + UUID.randomUUID() + validated.extension();
        String storedPath = storageService.store(file, fileName);
        
        ImagemVistoria img = new ImagemVistoria(
                vistoria,
                ambiente,
                categoria,
                storedPath,
                LocalDateTime.now(clock));
        
        vistoria.getImagens().add(img);
        try {
            return vistoriaRepository.saveAndFlush(vistoria);
        } catch (RuntimeException persistenceFailure) {
            vistoria.getImagens().remove(img);
            try {
                storageService.delete(storedPath);
            } catch (RuntimeException cleanupFailure) {
                persistenceFailure.addSuppressed(cleanupFailure);
            }
            if (persistenceFailure instanceof OptimisticLockingFailureException) {
                throw new StaleInspectionException();
            }
            throw persistenceFailure;
        }
    }

    private AmbienteVistoria validarAmbiente(
            Vistoria vistoria,
            RegistrarEvidenciaCommand command) {
        if (command == null || command.ambienteId() == null || command.ambienteId() <= 0) {
            throw new InvalidEvidenceException("Informe um ambiente válido para a evidência.");
        }
        return vistoria.getAmbientes().stream()
                .filter(ambiente -> Objects.equals(ambiente.getId(), command.ambienteId()))
                .findFirst()
                .orElseThrow(() -> new InvalidEvidenceException(
                        "O ambiente informado não pertence a esta vistoria."));
    }

    private CategoriaEvidencia validarCategoria(RegistrarEvidenciaCommand command) {
        String categoria = command == null ? null : command.categoria();
        if (categoria == null || categoria.isBlank()) {
            throw new InvalidEvidenceException("Informe a categoria da evidência.");
        }
        try {
            return CategoriaEvidencia.valueOf(categoria.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidEvidenceException(
                    "A categoria da evidência deve ser VISAO_GERAL ou DETALHE.");
        }
    }

    @Transactional
    public Vistoria submeterVistoria(Long vistoriaId, Usuario cliente) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        if (vistoria.getStatus() == VistoriaStatus.AGUARDANDO_IA) {
            return vistoria;
        }
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO
                && vistoria.getStatus() != VistoriaStatus.FALHA_IA) {
            throw new StaleInspectionException();
        }
        if (vistoria.getAmbientes().isEmpty() && vistoria.getImagens().isEmpty()) {
            throw new InvalidEvidenceException(
                    "Adicione ao menos uma evidência antes de enviar a vistoria.");
        }
        List<String> ambientesAusentes = vistoria.ambientesSemVisaoGeral();
        if (!ambientesAusentes.isEmpty()) {
            throw new IncompleteInspectionException(ambientesAusentes);
        }
        vistoria.iniciarAnalise();
        Vistoria saved = salvarComControleConcorrencia(vistoria);
        eventPublisher.publishEvent(new VistoriaSubmetidaEvent(saved.getId()));
        return saved;
    }

    @Transactional
    public Vistoria revisarAchado(
            Long vistoriaId,
            Usuario cliente,
            RevisarAchadoCommand command) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        if (vistoria.getStatus() != VistoriaStatus.REVISAO_PENDENTE) {
            throw new StaleInspectionException();
        }
        RevisarAchadoCommand normalized = validateReview(command);
        preLaudoParser.requireFinding(
                vistoria, normalized.imagemId(), normalized.indiceAchado());
        RevisaoAchado review = new RevisaoAchado(
                normalized.imagemId(),
                normalized.indiceAchado(),
                normalized.decisao(),
                normalized.contexto(),
                normalized.tipoCorrigido(),
                Instant.now(clock));
        vistoria.setRevisaoUsuario(reviewStore.upsert(vistoria.getRevisaoUsuario(), review));
        return salvarComControleConcorrencia(vistoria);
    }

    @Transactional
    public Vistoria concluirRelatorio(Long vistoriaId, Usuario cliente) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        if (vistoria.getStatus() == VistoriaStatus.RELATORIO_DISPONIVEL) {
            return vistoria;
        }
        if (vistoria.getStatus() != VistoriaStatus.REVISAO_PENDENTE) {
            throw new StaleInspectionException();
        }
        AnaliseVistoria analysis = preLaudoParser.parse(vistoria, vistoria.getPreLaudoIa());
        Set<String> reviewed = reviewStore.read(vistoria.getRevisaoUsuario()).stream()
                .map(review -> findingKey(review.imagemId(), review.indiceAchado()))
                .collect(Collectors.toSet());
        boolean incomplete = analysis.imagens().stream()
                .flatMap(image -> image.achados().stream()
                        .map(finding -> findingKey(image.imagemId(), finding.indice())))
                .anyMatch(key -> !reviewed.contains(key));
        if (incomplete) {
            throw new IncompleteReviewException();
        }
        vistoria.disponibilizarRelatorio(LocalDateTime.now(clock));
        return salvarComControleConcorrencia(vistoria);
    }

    private RevisarAchadoCommand validateReview(RevisarAchadoCommand command) {
        if (command == null || command.imagemId() == null || command.imagemId() <= 0
                || command.indiceAchado() == null || command.indiceAchado() < 0
                || command.decisao() == null) {
            throw new InvalidReviewException("A referência e a decisão do achado são obrigatórias.");
        }
        String context = trimToNull(command.contexto());
        if (context == null || context.length() > 1000) {
            throw new InvalidReviewException("O contexto deve ter entre 1 e 1000 caracteres.");
        }
        String correctedType = trimToNull(command.tipoCorrigido());
        if (command.decisao() == DecisaoRevisao.CORRIGIDO
                && (correctedType == null || correctedType.length() > 80)) {
            throw new InvalidReviewException("Informe o tipo corrigido do achado.");
        }
        if (correctedType != null && correctedType.length() > 80) {
            throw new InvalidReviewException("O tipo corrigido deve ter no máximo 80 caracteres.");
        }
        return new RevisarAchadoCommand(
                command.imagemId(),
                command.indiceAchado(),
                command.decisao(),
                context,
                command.decisao() == DecisaoRevisao.CORRIGIDO ? correctedType : null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String findingKey(long imageId, int findingIndex) {
        return imageId + ":" + findingIndex;
    }

    @Transactional(readOnly = true)
    public EvidenceContent buscarEvidencia(Long vistoriaId, Long imagemId, Usuario usuario) {
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(EvidenceNotFoundException::new);

        ImagemVistoria imagem = vistoria.getImagens().stream()
                .filter(item -> item.getId().equals(imagemId))
                .findFirst()
                .orElseThrow(EvidenceNotFoundException::new);

        autorizarLeitura(vistoria, usuario);
        StoredFile storedFile = storageService.load(imagem.getUrl());
        return new EvidenceContent(storedFile.resource(), storedFile.mediaType(), storedFile.length());
    }

    private void autorizarLeitura(Vistoria vistoria, Usuario usuario) {
        if (usuario.getPerfil() == PerfilEnum.ROLE_CLIENTE) {
            if (!vistoria.getCliente().getId().equals(usuario.getId())) {
                throw new EvidenceAccessDeniedException();
            }
            return;
        }

        if (usuario.getPerfil() == PerfilEnum.ROLE_ENGENHEIRO) {
            if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
                throw new StaleInspectionException();
            }
            return;
        }

        throw new EvidenceAccessDeniedException();
    }

    private Vistoria buscarPorIdEValidarCliente(Long vistoriaId, Usuario cliente) {
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(VistoriaNotFoundException::new);

        if (!vistoria.getCliente().getId().equals(cliente.getId())) {
            throw new VistoriaAccessDeniedException();
        }
        vistoria.getAmbientes().size();
        return vistoria;
    }

    @Transactional(readOnly = true)
    public Vistoria buscarVistoria(Long vistoriaId, Usuario usuario) {
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(VistoriaNotFoundException::new);
        autorizarAcessoAVistoria(vistoria, usuario);
        vistoria.getAmbientes().size();
        return vistoria;
    }

    /**
     * Mesma regra de {@link #autorizarLeitura}, mas lançando as exceções de
     * vistoria (não de evidência): este método serve a leitura do recurso
     * inteiro (GET /vistorias/{id}), não o conteúdo de um arquivo.
     */
    private void autorizarAcessoAVistoria(Vistoria vistoria, Usuario usuario) {
        if (usuario.getPerfil() == PerfilEnum.ROLE_CLIENTE) {
            if (!vistoria.getCliente().getId().equals(usuario.getId())) {
                throw new VistoriaAccessDeniedException();
            }
            return;
        }

        if (usuario.getPerfil() == PerfilEnum.ROLE_ENGENHEIRO) {
            if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
                throw new StaleInspectionException();
            }
            return;
        }

        throw new VistoriaAccessDeniedException();
    }

    /**
     * findByCliente/findByStatus não trazem `imagens` junto (ver comentário em
     * VistoriaRepository) para não paginar em memória. Busca-se aqui, em uma
     * segunda consulta com IN, apenas as vistorias da página já resolvida —
     * uma consulta extra por página, não uma por vistoria.
     */
    private Page<Vistoria> comImagensCarregadas(Page<Vistoria> pagina) {
        List<Long> ids = pagina.getContent().stream().map(Vistoria::getId).toList();
        if (ids.isEmpty()) {
            return pagina;
        }
        Map<Long, Vistoria> porId = vistoriaRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Vistoria::getId, Function.identity()));
        vistoriaRepository.findWithAmbientesByIdIn(ids);
        List<Vistoria> comImagens = ids.stream().map(porId::get).toList();
        return new PageImpl<>(comImagens, pagina.getPageable(), pagina.getTotalElements());
    }

    // Fluxo Engenheiro

    @Transactional(readOnly = true)
    public Page<Vistoria> listarPendentesEngenharia(Usuario engenheiro, Pageable pageable) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem listar pendentes");
        }
        return comImagensCarregadas(
                vistoriaRepository.findByStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO, pageable));
    }

    @Transactional
    public Vistoria aprovarVistoria(Long vistoriaId, Usuario engenheiro, String parecer) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem aprovar vistorias");
        }
        
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(VistoriaNotFoundException::new);

        if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
            throw new StaleInspectionException();
        }

        vistoria.setEngenheiro(engenheiro);
        vistoria.setParecerEngenheiro(parecer);
        vistoria.setStatus(VistoriaStatus.CONCLUIDA);
        vistoria.setDataConclusao(LocalDateTime.now());

        vistoria.getAmbientes().size();
        return salvarComControleConcorrencia(vistoria);
    }

    @Transactional
    public Vistoria devolverAoCliente(Long vistoriaId, Usuario engenheiro, String motivo) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem devolver vistorias");
        }

        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(VistoriaNotFoundException::new);

        if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
            throw new StaleInspectionException();
        }

        vistoria.setEngenheiro(engenheiro);
        vistoria.setParecerEngenheiro("Devolvido: " + motivo);
        vistoria.setStatus(VistoriaStatus.DEVOLVIDA_CLIENTE);

        vistoria.getAmbientes().size();
        return salvarComControleConcorrencia(vistoria);
    }

    private Vistoria salvarComControleConcorrencia(Vistoria vistoria) {
        try {
            return vistoriaRepository.saveAndFlush(vistoria);
        } catch (OptimisticLockingFailureException exception) {
            throw new StaleInspectionException();
        }
    }
}
