package br.com.vistoriapredial.vistoria.persistence;

import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.usuario.persistence.UsuarioRepository;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VistoriaRepositoryTest {

    @Autowired
    private VistoriaRepository vistoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldSaveVistoriaWithImagens() {
        Usuario cliente = usuarioRepository.save(new Usuario(
                "Cliente", "cliente-cascade@test.com", "hash", PerfilEnum.ROLE_CLIENTE, null));
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        ImagemVistoria imagem = new ImagemVistoria();
        imagem.setUrl("uploads/cascade.jpg");
        imagem.setProtocoloItem("SALA_PISO");
        imagem.setVistoria(vistoria);
        vistoria.getImagens().add(imagem);

        Vistoria saved = vistoriaRepository.saveAndFlush(vistoria);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(VistoriaStatus.EM_RASCUNHO);
        assertThat(saved.getImagens()).singleElement().extracting(ImagemVistoria::getId).isNotNull();
    }

    @Test
    void shouldPaginateByClienteAndByStatusWithoutEagerImagens() {
        Usuario cliente = usuarioRepository.saveAndFlush(new Usuario(
                "Cliente", "cliente-pagina@test.com", "hash", PerfilEnum.ROLE_CLIENTE, null));
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        ImagemVistoria imagem = new ImagemVistoria(
                vistoria, "uploads/a.jpg", "SALA_PISO", LocalDateTime.now());
        vistoria.getImagens().add(imagem);
        vistoriaRepository.saveAndFlush(vistoria);
        entityManager.clear();

        Page<Vistoria> porCliente = vistoriaRepository.findByCliente(cliente, PageRequest.of(0, 10));
        Page<Vistoria> porStatus = vistoriaRepository
                .findByStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO, PageRequest.of(0, 10));

        // Deliberado: sem @EntityGraph aqui (ver VistoriaRepository), então
        // "imagens" não vem carregado por essas duas consultas paginadas.
        assertThat(porCliente.getTotalElements()).isEqualTo(1);
        assertThat(porStatus.getTotalElements()).isEqualTo(1);
        assertThat(entityManagerFactory.getPersistenceUnitUtil()
                .isLoaded(porCliente.getContent().getFirst(), "imagens")).isFalse();
    }

    @Test
    void shouldLoadImagensEagerlyByIdIn() {
        Usuario cliente = usuarioRepository.saveAndFlush(new Usuario(
                "Cliente", "cliente-idin@test.com", "hash", PerfilEnum.ROLE_CLIENTE, null));
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        ImagemVistoria imagem = new ImagemVistoria(
                vistoria, "uploads/b.jpg", "SALA_PISO", LocalDateTime.now());
        vistoria.getImagens().add(imagem);
        Long id = vistoriaRepository.saveAndFlush(vistoria).getId();
        entityManager.clear();

        List<Vistoria> comImagens = vistoriaRepository.findByIdIn(List.of(id));

        assertThat(comImagens).hasSize(1);
        assertThat(entityManagerFactory.getPersistenceUnitUtil()
                .isLoaded(comImagens.getFirst(), "imagens")).isTrue();
        assertThat(comImagens.getFirst().getImagens()).hasSize(1);
    }

    @Test
    void shouldRejectStaleConcurrentUpdate() {
        Usuario cliente = usuarioRepository.saveAndFlush(new Usuario(
                "Cliente", "cliente-concorrencia@test.com", "hash", PerfilEnum.ROLE_CLIENTE, null));
        Vistoria vistoria = new Vistoria();
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.AGUARDANDO_ENGENHEIRO);
        Long id = vistoriaRepository.saveAndFlush(vistoria).getId();
        entityManager.clear();

        Vistoria staleCopy = vistoriaRepository.findById(id).orElseThrow();
        entityManager.detach(staleCopy);
        Vistoria current = vistoriaRepository.findById(id).orElseThrow();
        current.setStatus(VistoriaStatus.CONCLUIDA);
        vistoriaRepository.saveAndFlush(current);
        entityManager.clear();

        staleCopy.setStatus(VistoriaStatus.DEVOLVIDA_CLIENTE);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> vistoriaRepository.saveAndFlush(staleCopy)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
