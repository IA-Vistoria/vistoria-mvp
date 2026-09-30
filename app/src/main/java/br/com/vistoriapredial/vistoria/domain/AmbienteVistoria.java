package br.com.vistoriapredial.vistoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "tb_ambiente_vistoria")
public class AmbienteVistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vistoria_id", nullable = false)
    private Vistoria vistoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAmbiente tipo;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    private int ordem;

    protected AmbienteVistoria() {
    }

    private AmbienteVistoria(TipoAmbiente tipo, String nome, int ordem) {
        atualizar(tipo, nome, ordem);
    }

    public static AmbienteVistoria criar(TipoAmbiente tipo, String nome, int ordem) {
        return new AmbienteVistoria(tipo, nome, ordem);
    }

    void atualizar(TipoAmbiente tipo, String nome, int ordem) {
        if (tipo == null) {
            throw new RoteiroVistoriaInvalidoException("O tipo do ambiente é obrigatório.");
        }
        String nomeNormalizado = normalizarEspacos(nome);
        if (nomeNormalizado.length() < 2 || nomeNormalizado.length() > 60) {
            throw new RoteiroVistoriaInvalidoException(
                    "O nome do ambiente deve ter entre 2 e 60 caracteres.");
        }
        if (ordem < 0) {
            throw new RoteiroVistoriaInvalidoException("A ordem do ambiente é inválida.");
        }
        this.tipo = tipo;
        this.nome = nomeNormalizado;
        this.ordem = ordem;
    }

    void associar(Vistoria vistoria, int ordem) {
        this.vistoria = Objects.requireNonNull(vistoria, "A vistoria é obrigatória.");
        this.ordem = ordem;
    }

    private static String normalizarEspacos(String nome) {
        if (nome == null) {
            return "";
        }
        return nome.strip().replaceAll("\\s+", " ");
    }

    public Long getId() {
        return id;
    }

    public Vistoria getVistoria() {
        return vistoria;
    }

    public TipoAmbiente getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public int getOrdem() {
        return ordem;
    }
}
