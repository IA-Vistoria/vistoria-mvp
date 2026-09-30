package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.application.analysis.PreLaudoParser;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import org.springframework.stereotype.Component;

@Component
public class VistoriaResponseMapper {

    private final PreLaudoParser preLaudoParser;

    public VistoriaResponseMapper(PreLaudoParser preLaudoParser) {
        this.preLaudoParser = preLaudoParser;
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
                analysis
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
