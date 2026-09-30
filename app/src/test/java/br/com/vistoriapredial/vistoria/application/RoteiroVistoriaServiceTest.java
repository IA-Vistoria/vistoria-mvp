package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.usuario.domain.PerfilEnum;
import br.com.vistoriapredial.usuario.domain.Usuario;
import br.com.vistoriapredial.vistoria.application.analysis.PreLaudoParser;
import br.com.vistoriapredial.vistoria.application.command.AmbienteRoteiroCommand;
import br.com.vistoriapredial.vistoria.application.command.AtualizarRoteiroCommand;
import br.com.vistoriapredial.vistoria.application.command.CriarVistoriaCommand;
import br.com.vistoriapredial.vistoria.application.exception.StaleInspectionException;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchadoStore;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.persistence.VistoriaRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoteiroVistoriaServiceTest {

    private VistoriaRepository repository;
    private VistoriaService service;
    private Usuario cliente;

    @BeforeEach
    void setUp() {
        repository = mock(VistoriaRepository.class);
        var mapper = JsonMapper.builder().findAndAddModules().build();
        service = new VistoriaService(
                repository,
                mock(StorageService.class),
                mock(EvidenceFileValidator.class),
                mock(ApplicationEventPublisher.class),
                new PreLaudoParser(mapper),
                new RevisaoAchadoStore(mapper),
                Clock.systemUTC());
        cliente = new Usuario("Cliente", "cliente-roteiro@test.com", "hash", PerfilEnum.ROLE_CLIENTE, null);
        ReflectionTestUtils.setField(cliente, "id", 1L);
    }

    @Test
    void deveCriarVistoriaComRoteiroCompletoEmUmaOperacao() {
        when(repository.save(any(Vistoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vistoria criada = service.criarVistoria(cliente, new CriarVistoriaCommand(
                "Rua das Flores, 10",
                TipoImovel.APARTAMENTO,
                List.of(
                        new AmbienteRoteiroCommand(null, TipoAmbiente.SALA, "Sala"),
                        new AmbienteRoteiroCommand(null, TipoAmbiente.QUARTO, "Quarto"))));

        assertThat(criada.getEndereco()).isEqualTo("Rua das Flores, 10");
        assertThat(criada.getTipoImovel()).isEqualTo(TipoImovel.APARTAMENTO);
        assertThat(criada.getAmbientes())
                .extracting(AmbienteVistoria::getNome)
                .containsExactly("Sala", "Quarto");
        verify(repository).save(criada);
    }

    @Test
    void deveAtualizarRoteiroQuandoVersaoConfere() {
        Vistoria vistoria = vistoriaRascunho();
        AmbienteVistoria sala = vistoria.getAmbientes().getFirst();
        ReflectionTestUtils.setField(sala, "id", 11L);
        when(repository.findById(10L)).thenReturn(Optional.of(vistoria));
        when(repository.saveAndFlush(vistoria)).thenReturn(vistoria);

        Vistoria atualizada = service.atualizarRoteiro(10L, cliente, new AtualizarRoteiroCommand(
                3L,
                TipoImovel.CASA,
                List.of(
                        new AmbienteRoteiroCommand(11L, TipoAmbiente.SALA, "Sala integrada"),
                        new AmbienteRoteiroCommand(null, TipoAmbiente.VARANDA, "Varanda"))));

        assertThat(atualizada.getAmbientes())
                .extracting(AmbienteVistoria::getNome)
                .containsExactly("Sala integrada", "Varanda");
        verify(repository).saveAndFlush(vistoria);
    }

    @Test
    void deveRejeitarAtualizacaoComVersaoObsoleta() {
        Vistoria vistoria = vistoriaRascunho();
        when(repository.findById(10L)).thenReturn(Optional.of(vistoria));

        assertThatThrownBy(() -> service.atualizarRoteiro(10L, cliente,
                new AtualizarRoteiroCommand(2L, TipoImovel.CASA, List.of(
                        new AmbienteRoteiroCommand(null, TipoAmbiente.SALA, "Sala")))))
                .isInstanceOf(StaleInspectionException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    private Vistoria vistoriaRascunho() {
        Vistoria vistoria = new Vistoria();
        vistoria.setId(10L);
        vistoria.setCliente(cliente);
        vistoria.setStatus(VistoriaStatus.EM_RASCUNHO);
        vistoria.configurarRoteiro(TipoImovel.CASA, List.of(
                AmbienteVistoria.criar(TipoAmbiente.SALA, "Sala", 0)));
        ReflectionTestUtils.setField(vistoria, "version", 3L);
        return vistoria;
    }
}
