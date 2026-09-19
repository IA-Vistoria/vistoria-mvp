package br.com.vistoriapredial.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredFile(Resource resource, MediaType mediaType, long length) {
}
