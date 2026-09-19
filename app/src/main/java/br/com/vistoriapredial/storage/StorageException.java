package br.com.vistoriapredial.storage;

/**
 * Lançada quando uma operação de persistência de arquivo falha.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
