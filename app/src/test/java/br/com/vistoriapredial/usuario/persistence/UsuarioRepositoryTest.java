package br.com.vistoriapredial.usuario.persistence;

import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void shouldFindUserByEmail() {
        Usuario user = new Usuario("Test", "test@example.com", "hash", PerfilEnum.ROLE_CLIENTE, null);
        repository.saveAndFlush(user);

        Optional<Usuario> found = repository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Test");
    }

    @Test
    void shouldThrowExceptionWhenSavingDuplicateEmail() {
        Usuario user1 = new Usuario("Test 1", "duplicate@example.com", "hash", PerfilEnum.ROLE_CLIENTE, null);
        repository.saveAndFlush(user1);

        Usuario user2 = new Usuario("Test 2", "duplicate@example.com", "hash2", PerfilEnum.ROLE_ENGENHEIRO, "123");
        
        assertThatThrownBy(() -> repository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
