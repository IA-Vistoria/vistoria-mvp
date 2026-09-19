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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final StorageService storageService;
    private final IaIntegrationService iaIntegrationService;
    private final EvidenceFileValidator evidenceFileValidator;

    public VistoriaService(
            VistoriaRepository vistoriaRepository,
            StorageService storageService,
            IaIntegrationService iaIntegrationService,
            EvidenceFileValidator evidenceFileValidator) {
        this.vistoriaRepository = vistoriaRepository;
        this.storageService = storageService;
        this.iaIntegrationService = iaIntegrationService;
        this.evidenceFileValidator = evidenceFileValidator;
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
    public List<Vistoria> listarVistoriasCliente(Usuario cliente) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem listar suas vistorias");
        }
        return vistoriaRepository.findByCliente(cliente);
    }

    @Transactional
    public Vistoria uploadImagem(Long vistoriaId, Usuario cliente, String protocoloItem, MultipartFile file) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE) {
            throw new IllegalStateException("Vistoria não está em status que permita edição.");
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
            throw persistenceFailure;
        }
    }

    @Transactional
    public Vistoria submeterVistoria(Long vistoriaId, Usuario cliente) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE && vistoria.getStatus() != VistoriaStatus.FALHA_IA) {
            throw new IllegalStateException("Vistoria não pode ser submetida no status atual.");
        }
        
        if (vistoria.getImagens().isEmpty()) {
            throw new InvalidEvidenceException(
                    "Adicione ao menos uma evidência antes de enviar a vistoria.");
        }

        vistoria.setStatus(VistoriaStatus.AGUARDANDO_IA);
        vistoria = vistoriaRepository.save(vistoria);
        
        try {
            List<String> urls = vistoria.getImagens().stream()
                    .map(ImagemVistoria::getUrl)
                    .toList();
            String preLaudo = iaIntegrationService.analisarImagens(urls);
            vistoria.setPreLaudoIa(preLaudo);
            vistoria.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        } catch (Exception e) {
            vistoria.setStatus(VistoriaStatus.FALHA_IA);
        }
        
        return vistoriaRepository.save(vistoria);
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
                .orElseThrow(() -> new IllegalArgumentException("Vistoria não encontrada"));
        
        if (!vistoria.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Vistoria não pertence a este cliente");
        }
        return vistoria;
    }

    // Fluxo Engenheiro

    @Transactional(readOnly = true)
    public List<Vistoria> listarPendentesEngenharia(Usuario engenheiro) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem listar pendentes");
        }
        return vistoriaRepository.findByStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
    }

    @Transactional
    public Vistoria aprovarVistoria(Long vistoriaId, Usuario engenheiro, String parecer) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem aprovar vistorias");
        }
        
        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Vistoria não encontrada"));
        
        if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
            throw new IllegalStateException("Vistoria não está aguardando engenheiro.");
        }
        
        vistoria.setEngenheiro(engenheiro);
        vistoria.setParecerEngenheiro(parecer);
        vistoria.setStatus(VistoriaStatus.CONCLUIDA);
        vistoria.setDataConclusao(LocalDateTime.now());
        
        return vistoriaRepository.save(vistoria);
    }

    @Transactional
    public Vistoria devolverAoCliente(Long vistoriaId, Usuario engenheiro, String motivo) {
        if (engenheiro.getPerfil() != PerfilEnum.ROLE_ENGENHEIRO) {
            throw new IllegalArgumentException("Somente engenheiros podem devolver vistorias");
        }

        Vistoria vistoria = vistoriaRepository.findById(vistoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Vistoria não encontrada"));
        
        if (vistoria.getStatus() != VistoriaStatus.AGUARDANDO_ENGENHEIRO) {
            throw new IllegalStateException("Vistoria não está aguardando engenheiro.");
        }
        
        vistoria.setEngenheiro(engenheiro);
        vistoria.setParecerEngenheiro("Devolvido: " + motivo);
        vistoria.setStatus(VistoriaStatus.DEVOLVIDA_CLIENTE);
        
        return vistoriaRepository.save(vistoria);
    }
}
