package br.com.vistoriapredial.usuario.application.dto;

import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Deve validar sucesso com dados corretos")
    void deveValidarSucesso() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "João Silva",
                "joao@example.com",
                "12345678",
                PerfilEnum.ROLE_CLIENTE,
                null,
                null
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Deve falhar com email inválido e senha curta")
    void deveFalharEmailESenha() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "João Silva",
                "joao", // email inválido
                "123", // senha muito curta
                PerfilEnum.ROLE_CLIENTE,
                null,
                null
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}
