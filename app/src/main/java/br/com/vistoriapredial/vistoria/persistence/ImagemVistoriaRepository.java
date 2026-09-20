package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagemVistoriaRepository extends JpaRepository<ImagemVistoria, Long> {
}
