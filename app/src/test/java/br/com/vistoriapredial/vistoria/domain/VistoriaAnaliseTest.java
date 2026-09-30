package br.com.vistoriapredial.vistoria.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VistoriaAnaliseTest {

    private static final String DOCUMENTO_VALIDO = "{\"version\":2}";
    private static final LocalDateTime CONCLUIDA_EM =
            LocalDateTime.of(2026, 9, 30, 20, 0);

    @Test
    void deveDisponibilizarRelatorioAssimQueAAnaliseValidaForRegistrada() {
        Vistoria vistoria = aguardandoAnalise();

        vistoria.registrarAnalise(DOCUMENTO_VALIDO, CONCLUIDA_EM);

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
        assertThat(vistoria.getPreLaudoIa()).isEqualTo(DOCUMENTO_VALIDO);
        assertThat(vistoria.getDataConclusao()).isEqualTo(CONCLUIDA_EM);
    }

    @Test
    void deveRejeitarDocumentoVazioSemAlterarEstado() {
        Vistoria vistoria = aguardandoAnalise();

        assertThatThrownBy(() -> vistoria.registrarAnalise("   ", CONCLUIDA_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O documento validado da análise é obrigatório.");
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.AGUARDANDO_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
        assertThat(vistoria.getDataConclusao()).isNull();
    }

    @Test
    void deveRejeitarConclusaoSemInstanteSemAlterarEstado() {
        Vistoria vistoria = aguardandoAnalise();

        assertThatThrownBy(() -> vistoria.registrarAnalise(DOCUMENTO_VALIDO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O instante de conclusão da análise é obrigatório.");
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.AGUARDANDO_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
    }

    @Test
    void deveRegistrarFalhaSemFabricarDocumentoOuConclusao() {
        Vistoria vistoria = aguardandoAnalise();

        vistoria.falharAnalise();

        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.FALHA_IA);
        assertThat(vistoria.getPreLaudoIa()).isNull();
        assertThat(vistoria.getDataConclusao()).isNull();
    }

    @Test
    void deveRejeitarRegistroDeAnaliseForaDoEstadoEmAndamento() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);

        assertThatThrownBy(() -> vistoria.registrarAnalise(DOCUMENTO_VALIDO, CONCLUIDA_EM))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A vistoria não possui análise em andamento.");
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.EM_RASCUNHO);
    }

    @Test
    void deveRejeitarFalhaForaDoEstadoEmAndamento() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.RELATORIO_DISPONIVEL);
        vistoria.setPreLaudoIa(DOCUMENTO_VALIDO);
        vistoria.setDataConclusao(CONCLUIDA_EM);

        assertThatThrownBy(vistoria::falharAnalise)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A vistoria não possui análise em andamento.");
        assertThat(vistoria.getStatus()).isEqualTo(VistoriaStatus.RELATORIO_DISPONIVEL);
        assertThat(vistoria.getPreLaudoIa()).isEqualTo(DOCUMENTO_VALIDO);
        assertThat(vistoria.getDataConclusao()).isEqualTo(CONCLUIDA_EM);
    }

    private Vistoria aguardandoAnalise() {
        Vistoria vistoria = new Vistoria();
        vistoria.setStatus(VistoriaStatus.AGUARDANDO_IA);
        return vistoria;
    }
}
