package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.application.analysis.PreLaudoParser;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import org.springframework.stereotype.Component;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchadoStore;

@Component
public class VistoriaResponseMapper {

    private final PreLaudoParser preLaudoParser;
    private final RevisaoAchadoStore reviewStore;

    public VistoriaResponseMapper(PreLaudoParser preLaudoParser, RevisaoAchadoStore reviewStore) {
        this.preLaudoParser = preLaudoParser;
        this.reviewStore = reviewStore;
    }

    public VistoriaResponseDto toResponse(Vistoria vistoria) {
        AnaliseVistoria analysis = parseAnalysis(vistoria);
        return new VistoriaResponseDto(
                vistoria.getId(),
                vistoria.getCliente().getId(),
                vistoria.getStatus(),
                vistoria.getEndereco(),
                vistoria.getDataCriacao(),
                vistoria.getDataConclusao(),
                vistoria.getImagens().stream()
                        .map(image -> ImagemVistoriaResponseDto.from(vistoria.getId(), image))
                        .toList(),
                analysis,
                reviewStore.read(vistoria.getRevisaoUsuario())
        );
    }

    private AnaliseVistoria parseAnalysis(Vistoria vistoria) {
        String raw = vistoria.getPreLaudoIa();
        if (raw == null || raw.isBlank() || !raw.stripLeading().startsWith("{")) {
            return null;
        }
        return preLaudoParser.parse(vistoria, raw);
    }
}
