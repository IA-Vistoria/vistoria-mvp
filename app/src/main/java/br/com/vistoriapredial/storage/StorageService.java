package br.com.vistoriapredial.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para persistência de arquivos de foto de vistoria.
 *
 * <p>A implementação padrão em desenvolvimento utiliza o sistema de arquivos
 * local. Em produção, pode ser substituída por uma implementação OCI Object
 * Storage sem alterar o código dos Services que a consomem.</p>
 */
public interface StorageService {

    /**
     * Armazena o arquivo recebido e retorna o caminho relativo acessível.
     *
     * @param file     arquivo de imagem enviado pelo cliente
     * @param fileName nome de destino do arquivo (sem diretório)
     * @return caminho relativo ao diretório de uploads (ex: {@code "uploads/1_cozinha.jpg"})
     * @throws StorageException quando a gravação falha por motivo de I/O
     */
    String store(MultipartFile file, String fileName);

    /**
     * Exclui um arquivo previamente armazenado.
     *
     * @param relativePath caminho relativo retornado por {@link #store}
     */
    void delete(String relativePath);
}
