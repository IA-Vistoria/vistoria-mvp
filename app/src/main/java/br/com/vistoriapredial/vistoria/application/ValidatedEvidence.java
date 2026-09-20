package br.com.vistoriapredial.vistoria.application;

import org.springframework.http.MediaType;

public record ValidatedEvidence(String extension, MediaType mediaType) {
}
