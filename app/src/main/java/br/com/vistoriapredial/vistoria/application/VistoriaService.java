package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import br.com.vistoriapredial.vistoria.application.analysis.VistoriaSubmetidaEvent;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final StorageService storageService;
    private final EvidenceFileValidator evidenceFileValidator;
    private final ApplicationEventPublisher eventPublisher;

    public VistoriaService(
            VistoriaRepository vistoriaRepository,
            StorageService storageService,
            EvidenceFileValidator evidenceFileValidator,
            ApplicationEventPublisher eventPublisher) {
        this.vistoriaRepository = vistoriaRepository;
        this.storageService = storageService;
        this.evidenceFileValidator = evidenceFileValidator;
        this.eventPublisher = eventPublisher;
    }

    // Fluxo Cliente

    @Transactional
    public Vistoria criarVistoria(Usuario cliente, String endereco) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem criar vistorias");
        }
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setEndereco(endereco);
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        return vistoriaRepository.save(vistoria);
    }

    @Transactional(readOnly = true)
    public Page<Vistoria> listarVistoriasCliente(Usuario cliente, Pageable pageable) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem listar suas vistorias");
        }
        return comImagensCarregadas(vistoriaRepository.findByCliente(cliente, pageable));
    }

    @Transactional
    public Vistoria uploadImagem(Long vistoriaId, Usuario cliente, String protocoloItem, MultipartFile file) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE) {
            throw new StaleInspectionException();
        }

        if (protocoloItem == null || !ProtocoloVistoria.ITEMS.contains(protocoloItem)) {
            throw new InvalidEvidenceException("O item de protocolo informado é inválido.");
        }

        ValidatedEvidence validated = evidenceFileValidator.validate(file);
        String fileName = vistoriaId + "_" + UUID.randomUUID() + validated.extension();
        String storedPath = storageService.store(file, fileName);
        
        ImagemVistoria img = new ImagemVistoria();
        img.setUrl(storedPath);
        img.setProtocoloItem(protocoloItem);
        img.setVistoria(vistoria);
        
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
        if (vistoria.getImagens().isEmpty()) {
            throw new InvalidEvidenceException(
                    "Adicione ao menos uma evidência antes de enviar a vistoria.");
        }
        vistoria.iniciarAnalise();
        Vistoria saved = salvarComControleConcorrencia(vistoria);
        eventPublisher.publishEvent(new VistoriaSubmetidaEvent(saved.getId()));
        return saved;
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
        return vistoria;
    }

    @Transactional(readOnly = true)
    public Vistoria buscarVistoria(Long vistoriaId, Usuario usuario) {
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(VistoriaNotFoundException::new);
        autorizarAcessoAVistoria(vistoria, usuario);
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
