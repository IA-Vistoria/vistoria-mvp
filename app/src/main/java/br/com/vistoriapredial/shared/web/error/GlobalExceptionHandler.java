package br.com.vistoriapredial.shared.web.error;

import br.com.vistoriapredial.storage.StorageException;
import br.com.vistoriapredial.storage.StorageFileNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceAccessDeniedException;
import br.com.vistoriapredial.vistoria.application.exception.EvidenceNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.authentication.BadCredentialsException;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Centraliza a tradução de exceções da aplicação e do Spring MVC para
 * respostas HTTP no formato Problem Details (RFC 9457).
 *
 * <p>Com esse tratamento global, Controllers e Services permanecem responsáveis
 * apenas pelo fluxo normal, sem poluição de blocos {@code try/catch}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata erros de invariantes e validações do construtor de entidades.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.INVALID_DOMAIN_STATE,
                "Invariante de domínio violada",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }

    /**
     * Trata falhas na camada de persistência de arquivos.
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ProblemDetail> handleStorageException(
            StorageException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.STORAGE_ERROR,
                "Erro de armazenamento",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }

    @ExceptionHandler({EvidenceNotFoundException.class, StorageFileNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleEvidenceNotFound(
            RuntimeException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.NOT_FOUND,
                ProblemTypes.EVIDENCE_NOT_FOUND,
                "Evidência não encontrada",
                "A evidência solicitada não foi encontrada.",
                URI.create(request.getRequestURI())
        );
    }

    @ExceptionHandler(EvidenceAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleEvidenceAccessDenied(
            EvidenceAccessDeniedException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.FORBIDDEN,
                ProblemTypes.FORBIDDEN,
                "Acesso negado",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }

    @ExceptionHandler(StaleInspectionException.class)
    public ResponseEntity<ProblemDetail> handleStaleInspection(
            StaleInspectionException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.CONFLICT,
                ProblemTypes.STALE_INSPECTION,
                "Vistoria desatualizada",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }

    @ExceptionHandler(InvalidEvidenceException.class)
    public ResponseEntity<ProblemDetail> handleInvalidEvidence(
            InvalidEvidenceException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ProblemTypes.INVALID_EVIDENCE,
                "Evidência inválida",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ProblemTypes.INVALID_EVIDENCE,
                "Evidência inválida",
                "O arquivo de evidência deve ter no máximo 10 MB.",
                requestUri(request)
        );

        return createObjectResponse(problem, headers, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Trata violações das anotações de Bean Validation presentes nos DTOs.
     * O status 422 indica que o JSON foi compreendido, mas seus dados não atendem ao contrato.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valor inválido",
                        toJsonPointer(fieldError.getField())
                ))
                .sorted((first, second) -> first.pointer().compareTo(second.pointer()))
                .toList();

        ProblemDetail problem = createProblem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ProblemTypes.VALIDATION_ERROR,
                "Dados inválidos",
                "Um ou mais campos estão inválidos.",
                requestUri(request)
        );
        problem.setProperty("errors", errors);

        return createObjectResponse(problem, headers, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Trata corpos JSON malformados ou incompatíveis com o DTO esperado.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.INVALID_REQUEST,
                "Requisição inválida",
                "O corpo da requisição contém JSON inválido ou incompatível.",
                requestUri(request)
        );

        return createObjectResponse(problem, headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata parâmetros que não podem ser convertidos para o tipo declarado no Controller.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.INVALID_REQUEST,
                "Requisição inválida",
                "Um parâmetro da requisição possui formato inválido.",
                requestUri(request)
        );

        return createObjectResponse(problem, headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * Trata falhas inesperadas no servidor sem expor detalhes internos ao cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Erro não tratado ao processar {}", request.getRequestURI(), exception);

        return createResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemTypes.INTERNAL_ERROR,
                "Erro interno",
                "Ocorreu um erro interno inesperado.",
                URI.create(request.getRequestURI())
        );
    }

    private ResponseEntity<ProblemDetail> createResponse(
            HttpStatus status,
            URI type,
            String title,
            String detail,
            URI instance) {
        ProblemDetail problem = createProblem(status, type, title, detail, instance);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<Object> createObjectResponse(
            ProblemDetail problem,
            HttpHeaders headers,
            HttpStatus status) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        return new ResponseEntity<>(problem, responseHeaders, status);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            URI type,
            String title,
            String detail,
            URI instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setInstance(instance);
        return problem;
    }

    private URI requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return URI.create(servletWebRequest.getRequest().getRequestURI());
        }
        return URI.create("/");
    }

    private String toJsonPointer(String field) {
        return Stream.of(field.split("\\."))
                .map(this::escapeJsonPointerToken)
                .collect(Collectors.joining("/", "#/", ""));
    }

    private String escapeJsonPointerToken(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request) {
        return createResponse(
                HttpStatus.UNAUTHORIZED,
                ProblemTypes.UNAUTHORIZED,
                "Unauthorized",
                exception.getMessage(),
                URI.create(request.getRequestURI())
        );
    }
}
