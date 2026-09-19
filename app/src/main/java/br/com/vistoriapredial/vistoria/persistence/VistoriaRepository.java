package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {
    @EntityGraph(attributePaths = "imagens")
    List<Vistoria> findByStatus(VistoriaStatus status);

    @EntityGraph(attributePaths = "imagens")
    List<Vistoria> findByCliente(br.com.vistoriapredial.usuario.domain.Usuario cliente);

    @Override
    @EntityGraph(attributePaths = "imagens")
    Optional<Vistoria> findById(Long id);
}
