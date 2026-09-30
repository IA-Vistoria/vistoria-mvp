package br.com.vistoriapredial.vistoria.domain;

import br.com.vistoriapredial.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    @Column(name = "roteiro_revisao", nullable = false)
    private int roteiroRevisao;

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
        validarRoteiro(tipoImovel, ambientes);
        substituirAmbientes(tipoImovel, ambientes);
    }

    public void atualizarRoteiro(TipoImovel tipoImovel, List<ItemRoteiroVistoria> itens) {
        if (status != VistoriaStatus.EM_RASCUNHO) {
            throw new RoteiroVistoriaConflitoException(
                    "O roteiro só pode ser alterado enquanto a vistoria está em rascunho.");
        }
        if (itens == null) {
            throw new RoteiroVistoriaInvalidoException("O roteiro é obrigatório.");
        }

        Map<Long, AmbienteVistoria> atuaisPorId = new HashMap<>();
        for (AmbienteVistoria ambiente : ambientes) {
            if (ambiente.getId() != null) {
                atuaisPorId.put(ambiente.getId(), ambiente);
            }
        }

        Set<Long> idsMantidos = new HashSet<>();
        List<AtualizacaoAmbiente> atualizacoes = new ArrayList<>();
        List<AmbienteVistoria> estadoFinal = new ArrayList<>();
        for (int ordem = 0; ordem < itens.size(); ordem++) {
            ItemRoteiroVistoria item = itens.get(ordem);
            if (item == null) {
                throw new RoteiroVistoriaInvalidoException(
                        "O roteiro não pode conter um ambiente vazio.");
            }
            AmbienteVistoria dados = AmbienteVistoria.criar(item.tipo(), item.nome(), ordem);
            if (item.id() == null) {
                atualizacoes.add(new AtualizacaoAmbiente(null, dados));
                estadoFinal.add(dados);
                continue;
            }
            if (!idsMantidos.add(item.id())) {
                throw new RoteiroVistoriaInvalidoException(
                        "Um ambiente não pode aparecer mais de uma vez no roteiro.");
            }
            AmbienteVistoria existente = atuaisPorId.get(item.id());
            if (existente == null) {
                throw new RoteiroVistoriaInvalidoException(
                        "O ambiente informado não pertence a esta vistoria.");
            }
            atualizacoes.add(new AtualizacaoAmbiente(existente, dados));
            estadoFinal.add(dados);
        }

        validarRoteiro(tipoImovel, estadoFinal);
        ambientes.stream()
                .filter(ambiente -> ambiente.getId() != null && !idsMantidos.contains(ambiente.getId()))
                .filter(this::possuiEvidencia)
                .findFirst()
                .ifPresent(ambiente -> {
                    throw new RoteiroVistoriaConflitoException(
                            "O ambiente " + ambiente.getNome()
                                    + " possui evidências e não pode ser removido.");
                });

        List<AmbienteVistoria> reconciliados = new ArrayList<>();
        for (AtualizacaoAmbiente atualizacao : atualizacoes) {
            AmbienteVistoria ambiente = atualizacao.existente();
            if (ambiente == null) {
                ambiente = atualizacao.dados();
            } else {
                ambiente.atualizar(
                        atualizacao.dados().getTipo(),
                        atualizacao.dados().getNome(),
                        atualizacao.dados().getOrdem());
            }
            reconciliados.add(ambiente);
        }
        substituirAmbientes(tipoImovel, reconciliados);
    }

    private void validarRoteiro(TipoImovel tipoImovel, List<AmbienteVistoria> ambientes) {
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
    }

    private void substituirAmbientes(
            TipoImovel tipoImovel,
            List<AmbienteVistoria> ambientes) {
        this.tipoImovel = tipoImovel;
        this.roteiroRevisao++;
        this.ambientes.clear();
        for (int indice = 0; indice < ambientes.size(); indice++) {
            AmbienteVistoria ambiente = ambientes.get(indice);
            ambiente.associar(this, indice);
            this.ambientes.add(ambiente);
        }
    }

    private boolean possuiEvidencia(AmbienteVistoria ambiente) {
        return imagens.stream().anyMatch(imagem -> {
            AmbienteVistoria ambienteDaImagem = imagem.getAmbiente();
            if (ambienteDaImagem == ambiente) {
                return true;
            }
            return ambienteDaImagem != null
                    && ambienteDaImagem.getId() != null
                    && Objects.equals(ambienteDaImagem.getId(), ambiente.getId());
        });
    }

    public List<String> ambientesSemVisaoGeral() {
        return ambientes.stream()
                .filter(ambiente -> imagens.stream().noneMatch(imagem ->
                        imagem.getCategoria() == CategoriaEvidencia.VISAO_GERAL
                                && pertenceAoAmbiente(imagem, ambiente)))
                .map(AmbienteVistoria::getNome)
                .toList();
    }

    private boolean pertenceAoAmbiente(
            ImagemVistoria imagem,
            AmbienteVistoria ambiente) {
        AmbienteVistoria ambienteDaImagem = imagem.getAmbiente();
        if (ambienteDaImagem == ambiente) {
            return true;
        }
        return ambienteDaImagem != null
                && ambienteDaImagem.getId() != null
                && Objects.equals(ambienteDaImagem.getId(), ambiente.getId());
    }

    private record AtualizacaoAmbiente(
            AmbienteVistoria existente,
            AmbienteVistoria dados
    ) {
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

    public void registrarAnalise(String analiseOriginal, LocalDateTime concludedAt) {
        exigirAnaliseEmAndamento();
        if (analiseOriginal == null || analiseOriginal.isBlank()) {
            throw new IllegalArgumentException("O documento validado da análise é obrigatório.");
        }
        if (concludedAt == null) {
            throw new IllegalArgumentException("O instante de conclusão da análise é obrigatório.");
        }
        preLaudoIa = analiseOriginal;
        dataConclusao = concludedAt;
        status = VistoriaStatus.RELATORIO_DISPONIVEL;
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
