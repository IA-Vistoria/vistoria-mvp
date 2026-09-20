package br.com.vistoriapredial.usuario.application.dto;

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

class LoginRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Deve validar sucesso com credenciais válidas")
    void deveValidarSucesso() {
        LoginRequestDto dto = new LoginRequestDto("joao@example.com", "123456");

        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Deve falhar com credenciais inválidas")
    void deveFalharCredenciais() {
        LoginRequestDto dto = new LoginRequestDto("", ""); // vazios

        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}
