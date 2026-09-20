package br.com.vistoriapredial.vistoria.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação próprio, em vez de serializar {@link Page} diretamente.
 * Mantém o contrato HTTP estável e explícito, independente da representação
 * JSON que a versão do Spring Data usar internamente para {@code Page}.
 */
public record PaginaResponseDto<T>(
        List<T> content,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {
    public static <T, S> PaginaResponseDto<T> from(Page<S> page, Function<S, T> mapper) {
        return new PaginaResponseDto<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
