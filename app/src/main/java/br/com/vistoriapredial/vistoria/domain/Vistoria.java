package br.com.vistoriapredial.vistoria.domain;

import br.com.vistoriapredial.usuario.domain.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_vistoria")
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public List<ImagemVistoria> getImagens() {
        return imagens;
    }

    public void setImagens(List<ImagemVistoria> imagens) {
        this.imagens = imagens;
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

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
        if (this.status == null) {
            this.status = VistoriaStatus.EM_RASCUNHO;
        }
    }
}
