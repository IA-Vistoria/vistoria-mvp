package br.com.vistoriapredial.vistoria.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VistoriaRoteiroTest {

    @Test
    void deveConfigurarRoteiroComTipoEOrdemDosAmbientes() {
        Vistoria vistoria = new Vistoria();

        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0),
                AmbienteVistoria.criar(TipoAmbiente.QUARTO, "Quarto principal", 1)));

        assertThat(vistoria.getTipoImovel()).isEqualTo(TipoImovel.APARTAMENTO);
        assertThat(vistoria.getAmbientes())
                .extracting(AmbienteVistoria::getNome)
                .containsExactly("Sala", "Quarto principal");
        assertThat(vistoria.getAmbientes())
                .extracting(AmbienteVistoria::getOrdem)
                .containsExactly(0, 1);
        assertThat(vistoria.getAmbientes())
                .allSatisfy(ambiente -> assertThat(ambiente.getVistoria()).isSameAs(vistoria));
    }

    @Test
    void deveAceitarRoteiroComTrintaAmbientes() {
        Vistoria vistoria = new Vistoria();
        List<AmbienteVistoria> ambientes = IntStream.range(0, 30)
                .mapToObj(ordem -> AmbienteVistoria.criar(
                        TipoAmbiente.OUTRO, "Ambiente " + ordem, ordem))
                .toList();

        vistoria.configurarRoteiro(TipoImovel.COMERCIAL, ambientes);

        assertThat(vistoria.getAmbientes()).hasSize(30);
    }

    @Test
    void deveRejeitarRoteiroVazio() {
        Vistoria vistoria = new Vistoria();

        assertThatThrownBy(() -> vistoria.configurarRoteiro(TipoImovel.CASA, List.of()))
                .isInstanceOf(RoteiroVistoriaInvalidoException.class)
                .hasMessageContaining("ao menos um ambiente");
    }

    @Test
    void deveRejeitarRoteiroComMaisDeTrintaAmbientes() {
        Vistoria vistoria = new Vistoria();
        List<AmbienteVistoria> ambientes = IntStream.range(0, 31)
                .mapToObj(ordem -> AmbienteVistoria.criar(
                        TipoAmbiente.OUTRO, "Ambiente " + ordem, ordem))
                .toList();

        assertThatThrownBy(() -> vistoria.configurarRoteiro(TipoImovel.OUTRO, ambientes))
                .isInstanceOf(RoteiroVistoriaInvalidoException.class)
                .hasMessageContaining("30 ambientes");
    }

    @Test
    void deveRejeitarNomesDuplicadosIgnorandoCaixaEEspacos() {
        Vistoria vistoria = new Vistoria();

        assertThatThrownBy(() -> vistoria.configurarRoteiro(TipoImovel.CASA, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala de estar", 0),
                AmbienteVistoria.criar(TipoAmbiente.SALA, "  SALA   DE ESTAR ", 1))))
                .isInstanceOf(RoteiroVistoriaInvalidoException.class)
                .hasMessageContaining("nomes diferentes");
    }

    @Test
    void deveRejeitarNomeForaDoLimite() {
        assertThatThrownBy(() -> AmbienteVistoria.criar(TipoAmbiente.OUTRO, "A", 0))
                .isInstanceOf(RoteiroVistoriaInvalidoException.class)
                .hasMessageContaining("2 e 60");
    }

    @Test
    void deveAssociarImagemNovaAoAmbienteECategoriaMantendoIdentificadorLegado() {
        Vistoria vistoria = new Vistoria();
        AmbienteVistoria sala = AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0);
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(sala));

        ImagemVistoria imagem = new ImagemVistoria(
                vistoria,
                sala,
                CategoriaEvidencia.VISAO_GERAL,
                "uploads/sala.jpg",
                LocalDateTime.of(2026, 9, 30, 12, 0));

        assertThat(imagem.getAmbiente()).isSameAs(sala);
        assertThat(imagem.getCategoria()).isEqualTo(CategoriaEvidencia.VISAO_GERAL);
        assertThat(imagem.getProtocoloItem()).isEqualTo("SALA_VISAO_GERAL");
    }

    @Test
    void deveManterImagemLegadaLegivelSemAmbienteEstruturado() {
        Vistoria vistoria = new Vistoria();

        ImagemVistoria imagem = new ImagemVistoria(
                vistoria,
                "uploads/legada.jpg",
                "SALA_PAREDES_REVESTIMENTOS",
                LocalDateTime.of(2025, 1, 10, 10, 0));

        assertThat(imagem.getAmbiente()).isNull();
        assertThat(imagem.getCategoria()).isNull();
        assertThat(imagem.getProtocoloItem()).isEqualTo("SALA_PAREDES_REVESTIMENTOS");
    }

    @Test
    void deveAdicionarRenomearReordenarERemoverAmbienteSemEvidencia() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        AmbienteVistoria sala = AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0);
        AmbienteVistoria quarto = AmbienteVistoria.criar(TipoAmbiente.QUARTO, "Quarto", 1);
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(sala, quarto));
        definirId(sala, 11L);
        definirId(quarto, 12L);

        vistoria.atualizarRoteiro(TipoImovel.APARTAMENTO, List.of(
                new ItemRoteiroVistoria(12L, TipoAmbiente.QUARTO, "Suíte"),
                new ItemRoteiroVistoria(null, TipoAmbiente.ESCRITORIO, "Escritório")));

        assertThat(vistoria.getAmbientes())
                .extracting(AmbienteVistoria::getNome)
                .containsExactly("Suíte", "Escritório");
        assertThat(vistoria.getAmbientes())
                .extracting(AmbienteVistoria::getOrdem)
                .containsExactly(0, 1);
    }

    @Test
    void deveRejeitarRemocaoDeAmbienteComEvidencia() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        AmbienteVistoria sala = AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0);
        AmbienteVistoria quarto = AmbienteVistoria.criar(TipoAmbiente.QUARTO, "Quarto", 1);
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(sala, quarto));
        definirId(sala, 11L);
        definirId(quarto, 12L);
        vistoria.getImagens().add(new ImagemVistoria(
                vistoria, sala, CategoriaEvidencia.VISAO_GERAL,
                "uploads/sala.jpg", LocalDateTime.now()));

        assertThatThrownBy(() -> vistoria.atualizarRoteiro(TipoImovel.APARTAMENTO, List.of(
                new ItemRoteiroVistoria(12L, TipoAmbiente.QUARTO, "Quarto"))))
                .isInstanceOf(RoteiroVistoriaConflitoException.class)
                .hasMessageContaining("Sala");
    }

    @Test
    void deveRejeitarEdicaoDoRoteiroForaDoRascunho() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.AGUARDANDO_IA);
        AmbienteVistoria sala = AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0);
        vistoria.configurarRoteiro(TipoImovel.CASA, List.of(sala));

        assertThatThrownBy(() -> vistoria.atualizarRoteiro(TipoImovel.CASA, List.of(
                new ItemRoteiroVistoria(null, TipoAmbiente.SALA, "Sala"))))
                .isInstanceOf(RoteiroVistoriaConflitoException.class)
                .hasMessageContaining("rascunho");
    }

    @Test
    void deveRejeitarIdentificadorDeAmbienteQueNaoPertenceAoRoteiro() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        vistoria.configurarRoteiro(TipoImovel.CASA, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0)));

        assertThatThrownBy(() -> vistoria.atualizarRoteiro(TipoImovel.CASA, List.of(
                new ItemRoteiroVistoria(999L, TipoAmbiente.SALA, "Sala"))))
                .isInstanceOf(RoteiroVistoriaInvalidoException.class)
                .hasMessageContaining("não pertence");
    }

    private void definirId(AmbienteVistoria ambiente, Long id) {
        try {
            var field = AmbienteVistoria.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ambiente, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
