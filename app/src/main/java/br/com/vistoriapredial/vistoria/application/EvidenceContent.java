package br.com.vistoriapredial.vistoria.application;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record EvidenceContent(Resource resource, MediaType mediaType, long length) {
}
