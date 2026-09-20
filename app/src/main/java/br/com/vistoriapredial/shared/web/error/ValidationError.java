package br.com.vistoriapredial.shared.web.error;

/**
 * Representa uma violação de validação associada a um campo da requisição.
 *
 * @param detail mensagem legível que explica a violação
 * @param pointer localização do campo no documento JSON, no padrão RFC 6901
 */
public record ValidationError(String detail, String pointer) {
}
