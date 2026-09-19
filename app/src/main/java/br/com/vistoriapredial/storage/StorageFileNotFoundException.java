package br.com.vistoriapredial.storage;

public class StorageFileNotFoundException extends StorageException {

    public StorageFileNotFoundException() {
        super("Arquivo de evidência não encontrado.");
    }
}
