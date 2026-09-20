package br.com.vistoriapredial.usuario.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UsuarioTest {

    @Test
    void shouldCreateValidUsuario() {
        Usuario usuario = new Usuario("John", "john@example.com", "hash", PerfilEnum.ROLE_CLIENTE, null);
        assertThat(usuario.getNome()).isEqualTo("John");
        assertThat(usuario.getEmail()).isEqualTo("john@example.com");
        assertThat(usuario.getSenha()).isEqualTo("hash");
        assertThat(usuario.getPerfil()).isEqualTo(PerfilEnum.ROLE_CLIENTE);
        assertThat(usuario.getCrea()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        assertThatThrownBy(() -> new Usuario("", "john@example.com", "hash", PerfilEnum.ROLE_CLIENTE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome não pode ser vazio");
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        assertThatThrownBy(() -> new Usuario("John", "", "hash", PerfilEnum.ROLE_CLIENTE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email não pode ser vazio");
    }

    @Test
    void shouldThrowExceptionWhenSenhaIsBlank() {
        assertThatThrownBy(() -> new Usuario("John", "john@example.com", "", PerfilEnum.ROLE_CLIENTE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha não pode ser vazia");
    }

    @Test
    void shouldThrowExceptionWhenPerfilIsNull() {
        assertThatThrownBy(() -> new Usuario("John", "john@example.com", "hash", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil não pode ser nulo");
    }
}
