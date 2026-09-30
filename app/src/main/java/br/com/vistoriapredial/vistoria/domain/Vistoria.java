package br.com.vistoriapredial.vistoria.domain;

import br.com.vistoriapredial.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "tb_vistoria")
public class Vistoria {

    public static final int MAXIMO_AMBIENTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engenheiro_id")
    private Usuario engenheiro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VistoriaStatus status;

    @Column(columnDefinition = "TEXT")
    private String preLaudoIa;

    @Column(columnDefinition = "TEXT")
    private String parecerEngenheiro;

    @Column(name = "revisao_usuario", columnDefinition = "TEXT")
    private String revisaoUsuario;

    @Column(length = 255)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imovel", length = 20)
    private TipoImovel tipoImovel;

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AmbienteVistoria> ambientes = new ArrayList<>();

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagemVistoria> imagens = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataConclusao;

    public Vistoria() {
    }

    public Vistoria(Usuario cliente, Usuario engenheiro, VistoriaStatus status, String preLaudoIa, String parecerEngenheiro, List<ImagemVistoria> imagens, LocalDateTime dataCriacao, LocalDateTime dataConclusao) {
        this.cliente = cliente;
        this.engenheiro = engenheiro;
        this.status = status;
        this.preLaudoIa = preLaudoIa;
        this.parecerEngenheiro = parecerEngenheiro;
        this.imagens = imagens;
        this.dataCriacao = dataCriacao;
        this.dataConclusao = dataConclusao;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Usuario getEngenheiro() {
        return engenheiro;
    }

    public void setEngenheiro(Usuario engenheiro) {
        this.engenheiro = engenheiro;
    }

    public VistoriaStatus getStatus() {
        return status;
    }

    public void setStatus(VistoriaStatus status) {
        this.status = status;
    }

    public String getPreLaudoIa() {
        return preLaudoIa;
    }

    public void setPreLaudoIa(String preLaudoIa) {
        this.preLaudoIa = preLaudoIa;
    }

    public String getParecerEngenheiro() {
        return parecerEngenheiro;
    }

    public void setParecerEngenheiro(String parecerEngenheiro) {
        this.parecerEngenheiro = parecerEngenheiro;
    }

    public String getRevisaoUsuario() {
        return revisaoUsuario;
    }

    public void setRevisaoUsuario(String revisaoUsuario) {
        this.revisaoUsuario = revisaoUsuario;
    }

    public List<ImagemVistoria> getImagens() {
        return imagens;
    }

    public void setImagens(List<ImagemVistoria> imagens) {
        this.imagens = imagens;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public TipoImovel getTipoImovel() {
        return tipoImovel;
    }

    public List<AmbienteVistoria> getAmbientes() {
        return List.copyOf(ambientes);
    }

    public void configurarRoteiro(TipoImovel tipoImovel, List<AmbienteVistoria> ambientes) {
        if (tipoImovel == null) {
            throw new RoteiroVistoriaInvalidoException("O tipo do imóvel é obrigatório.");
        }
        if (ambientes == null || ambientes.isEmpty()) {
            throw new RoteiroVistoriaInvalidoException(
                    "O roteiro deve possuir ao menos um ambiente.");
        }
        if (ambientes.size() > MAXIMO_AMBIENTES) {
            throw new RoteiroVistoriaInvalidoException(
                    "O roteiro deve possuir no máximo 30 ambientes.");
        }

        Set<String> nomes = new HashSet<>();
        for (AmbienteVistoria ambiente : ambientes) {
            if (ambiente == null) {
                throw new RoteiroVistoriaInvalidoException(
                        "O roteiro não pode conter um ambiente vazio.");
            }
            String chave = ambiente.getNome().toLowerCase(Locale.ROOT);
            if (!nomes.add(chave)) {
                throw new RoteiroVistoriaInvalidoException(
                        "Os ambientes do roteiro devem possuir nomes diferentes.");
            }
        }

        this.tipoImovel = tipoImovel;
        this.ambientes.clear();
        for (int indice = 0; indice < ambientes.size(); indice++) {
            AmbienteVistoria ambiente = ambientes.get(indice);
            ambiente.associar(this, indice);
            this.ambientes.add(ambiente);
        }
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public boolean iniciarAnalise() {
        if (status == VistoriaStatus.AGUARDANDO_IA) {
            return false;
        }
        if (status != VistoriaStatus.EM_RASCUNHO && status != VistoriaStatus.FALHA_IA) {
            throw new IllegalStateException("A vistoria não pode ser submetida neste estado.");
        }
        preLaudoIa = null;
        dataConclusao = null;
        status = VistoriaStatus.AGUARDANDO_IA;
        return true;
    }

    public void registrarAnalise(String analiseOriginal) {
        exigirAnaliseEmAndamento();
        preLaudoIa = analiseOriginal;
        dataConclusao = null;
        status = VistoriaStatus.REVISAO_PENDENTE;
    }

    public void falharAnalise() {
        exigirAnaliseEmAndamento();
        preLaudoIa = null;
        dataConclusao = null;
        status = VistoriaStatus.FALHA_IA;
    }

    private void exigirAnaliseEmAndamento() {
        if (status != VistoriaStatus.AGUARDANDO_IA) {
            throw new IllegalStateException("A vistoria não possui análise em andamento.");
        }
    }

    public void disponibilizarRelatorio(LocalDateTime concludedAt) {
        if (status != VistoriaStatus.REVISAO_PENDENTE) {
            throw new IllegalStateException("A vistoria não está pronta para gerar o relatório.");
        }
        status = VistoriaStatus.RELATORIO_DISPONIVEL;
        dataConclusao = concludedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
        if (this.status == null) {
            this.status = VistoriaStatus.EM_RASCUNHO;
        }
    }
}
