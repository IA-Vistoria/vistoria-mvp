package br.com.vistoriapredial.usuario.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilEnum perfil;

    @Column(length = 30)
    private String crea;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Usuario() {
        // required by JPA
    }

    public Usuario(String nome, String email, String senha, PerfilEnum perfil, String crea) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email não pode ser vazio");
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("Senha não pode ser vazia");
        if (perfil == null) throw new IllegalArgumentException("Perfil não pode ser nulo");
        
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.crea = crea;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public PerfilEnum getPerfil() { return perfil; }
    public String getCrea() { return crea; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
