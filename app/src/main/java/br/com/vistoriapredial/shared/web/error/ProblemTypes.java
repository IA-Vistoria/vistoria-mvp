package br.com.vistoriapredial.shared.web.error;

import java.net.URI;

/**
 * Catálogo central dos identificadores de problemas expostos pela API (RFC 9457).
 *
 * <p>As URNs funcionam como identificadores estáveis do contrato HTTP e não
 * dependem da existência de um domínio público.</p>
 */
public final class ProblemTypes {

    public static final URI USUARIO_NOT_FOUND =
            URI.create("urn:vistoria:problem:usuario-not-found");
    public static final URI EMAIL_ALREADY_EXISTS =
            URI.create("urn:vistoria:problem:email-already-exists");

    public static final URI INVALID_DOMAIN_STATE =
            URI.create("urn:vistoria:problem:invalid-domain-state");
    public static final URI INVALID_REQUEST =
            URI.create("urn:vistoria:problem:invalid-request");
    public static final URI UNAUTHORIZED =
            URI.create("urn:vistoria:problem:unauthorized");
    public static final URI VALIDATION_ERROR =
            URI.create("urn:vistoria:problem:validation-error");
    public static final URI STORAGE_ERROR =
            URI.create("urn:vistoria:problem:storage-error");
    public static final URI INVALID_EVIDENCE =
            URI.create("urn:vistoria:problem:invalid-evidence");
    public static final URI INTERNAL_ERROR =
            URI.create("urn:vistoria:problem:internal-error");

    private ProblemTypes() {
    }
}
