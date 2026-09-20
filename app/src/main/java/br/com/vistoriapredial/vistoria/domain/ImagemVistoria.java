package br.com.vistoriapredial.vistoria.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_imagem_vistoria")
public class ImagemVistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vistoria_id", nullable = false)
    private Vistoria vistoria;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, length = 100)
    private String protocoloItem;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataUpload;

    public ImagemVistoria() {
    }

    public ImagemVistoria(Vistoria vistoria, String url, String protocoloItem, LocalDateTime dataUpload) {
        this.vistoria = vistoria;
        this.url = url;
        this.protocoloItem = protocoloItem;
        this.dataUpload = dataUpload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vistoria getVistoria() {
        return vistoria;
    }

    public void setVistoria(Vistoria vistoria) {
        this.vistoria = vistoria;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocoloItem() {
        return protocoloItem;
    }

    public void setProtocoloItem(String protocoloItem) {
        this.protocoloItem = protocoloItem;
    }

    public LocalDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(LocalDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    @PrePersist
    protected void onCreate() {
        this.dataUpload = LocalDateTime.now();
    }
}
