package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {

    /*
     * Sem @EntityGraph aqui de propósito: combinar fetch join de coleção com
     * Pageable faz o Hibernate paginar em memória (carrega a tabela toda e
     * pagina em Java), anulando o índice e o LIMIT/OFFSET no banco. As
     * imagens de cada página são carregadas depois via findByIdIn.
     */
    Page<Vistoria> findByStatus(VistoriaStatus status, Pageable pageable);

    Page<Vistoria> findByCliente(Usuario cliente, Pageable pageable);

    @EntityGraph(attributePaths = "imagens")
    List<Vistoria> findByIdIn(List<Long> ids);

    @Override
    @EntityGraph(attributePaths = "imagens")
    Optional<Vistoria> findById(Long id);
}
