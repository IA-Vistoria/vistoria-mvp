package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VistoriaRepository extends JpaRepository<Vistoria, Long> {
    List<Vistoria> findByStatus(VistoriaStatus status);
}
