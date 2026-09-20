package br.com.vistoriapredial.usuario.application.dto;

public record AuthResponseDto(
        String token,
        String tipo,
        Long usuarioId,
        String nome,
        String perfil
) {
    public AuthResponseDto(String token, Long usuarioId, String nome, String perfil) {
        this(token, "Bearer", usuarioId, nome, perfil);
    }
}
