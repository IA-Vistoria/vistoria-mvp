package br.com.vistoriapredial.usuario.application.dto;

import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ser válido")
        @Size(max = 100, message = "O email deve ter no máximo 100 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull(message = "O perfil é obrigatório")
        PerfilEnum perfil,

        @Size(max = 20, message = "O CREA deve ter no máximo 20 caracteres")
        String crea,

        @Size(max = 128, message = "O código de convite deve ter no máximo 128 caracteres")
        String codigoConvite
) {
}
