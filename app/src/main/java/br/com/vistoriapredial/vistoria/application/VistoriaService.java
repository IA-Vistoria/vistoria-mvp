package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VistoriaService {

    private final VistoriaRepository vistoriaRepository;
    private final StorageService storageService;
    private final IaIntegrationService iaIntegrationService;

    public VistoriaService(VistoriaRepository vistoriaRepository, StorageService storageService, IaIntegrationService iaIntegrationService) {
        this.vistoriaRepository = vistoriaRepository;
        this.storageService = storageService;
        this.iaIntegrationService = iaIntegrationService;
    }

    // Fluxo Cliente

    @Transactional
    public Vistoria criarVistoria(Usuario cliente) {
        if (cliente.getPerfil() != PerfilEnum.ROLE_CLIENTE) {
            throw new IllegalArgumentException("Somente clientes podem criar vistorias");
        }
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        return vistoriaRepository.save(vistoria);
    }

    @Transactional
    public ImagemVistoria uploadImagem(Long vistoriaId, Usuario cliente, String protocoloItem, MultipartFile file) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE) {
            throw new IllegalStateException("Vistoria não está em status que permita edição.");
        }

        String fileName = vistoriaId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String url = storageService.store(file, fileName);
        
        ImagemVistoria img = new ImagemVistoria();
        img.setUrl(url);
        img.setProtocoloItem(protocoloItem);
        img.setVistoria(vistoria);
        
        vistoria.getImagens().add(img);
        vistoriaRepository.save(vistoria);
        return img;
    }

    @Transactional
    public Vistoria submeterVistoria(Long vistoriaId, Usuario cliente) {
        Vistoria vistoria = buscarPorIdEValidarCliente(vistoriaId, cliente);
        
        if (vistoria.getStatus() != VistoriaStatus.EM_RASCUNHO && vistoria.getStatus() != VistoriaStatus.DEVOLVIDA_CLIENTE && vistoria.getStatus() != VistoriaStatus.FALHA_IA) {
            throw new IllegalStateException("Vistoria não pode ser submetida no status atual.");
        }
        
        if (vistoria.getImagens().isEmpty()) {
            throw new IllegalStateException("Vistoria precisa de pelo menos uma imagem para ser submetida.");
        }

        vistoria.setStatus(VistoriaStatus.AGUARDANDO_IA);
        vistoria = vistoriaRepository.save(vistoria);
        
        try {
            List<String> urls = vistoria.getImagens().stream().map(ImagemVistoria::getUrl).collect(Collectors.toList());
            String preLaudo = iaIntegrationService.analisarImagens(urls);
            vistoria.setPreLaudoIa(preLaudo);
            vistoria.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        } catch (Exception e) {
            vistoria.setStatus(VistoriaStatus.FALHA_IA);
        }
        
        return vistoriaRepository.save(vistoria);
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
