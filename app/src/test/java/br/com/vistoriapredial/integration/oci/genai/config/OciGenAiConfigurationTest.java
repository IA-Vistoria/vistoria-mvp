package br.com.vistoriapredial.integration.oci.genai.config;

import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.retrier.RetryConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OciGenAiConfigurationTest {

    @Test
    void applicationPropertiesNaoDeveSobrescreverDefaultsComConfiguracaoLegada() throws IOException {
        Properties application = new Properties();
        Path applicationProperties = Path.of("src", "main", "resources", "application.properties");
        try (InputStream resource = Files.newInputStream(applicationProperties)) {
            application.load(resource);
        }

        assertThat(application.getProperty("oci.genai.region"))
                .isEqualTo("${OCI_REGION:sa-saopaulo-1}");
        assertThat(application.getProperty("oci.genai.model-id"))
                .isEqualTo("${OCI_GENAI_MODEL_ID:google.gemini-2.5-flash}");
        assertThat(application.getProperty("oci.genai.compartment-id"))
                .isEqualTo("${OCI_COMPARTMENT_ID:}");
        assertThat(application).doesNotContainKey("oci.genai.endpoint");
    }

    @Test
    void deveUsarSaoPauloEGeminiComoPadroes() {
        OciGenAiProperties properties = new OciGenAiProperties();

        assertThat(properties.getRegion()).isEqualTo("sa-saopaulo-1");
        assertThat(properties.getModelId()).isEqualTo("google.gemini-2.5-flash");
        assertThat(properties.getAuthMode()).isEqualTo(OciGenAiProperties.AuthMode.CONFIG_FILE);
    }

    @Test
    void deveExigirCompartmentQuandoOciForAtivado() {
        OciGenAiProperties properties = new OciGenAiProperties();

        assertThatThrownBy(properties::validarParaUso)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OCI_COMPARTMENT_ID é obrigatório para o provider OCI.");
    }

    @Test
    void deveRejeitarTimeoutInvalido() {
        OciGenAiProperties properties = propriedadesValidas();
        properties.setReadTimeoutMs(0);

        assertThatThrownBy(properties::validarParaUso)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Os timeouts do OCI devem ser maiores que zero.");
    }

    @Test
    void deveConstruirProviderDeArquivoComCaminhoEProfileConfigurados() {
        AbstractAuthenticationDetailsProvider expected = mock(AbstractAuthenticationDetailsProvider.class);
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> profile = new AtomicReference<>();
        OciAuthenticationProviderFactory factory = new OciAuthenticationProviderFactory(
                (configuredPath, configuredProfile) -> {
                    path.set(configuredPath);
                    profile.set(configuredProfile);
                    return expected;
                },
                () -> mock(AbstractAuthenticationDetailsProvider.class));
        OciGenAiProperties properties = propriedadesValidas();
        properties.setConfigFile("C:/oci/config");
        properties.setConfigProfile("VISTORIA");

        AbstractAuthenticationDetailsProvider result = factory.criar(properties);

        assertThat(result).isSameAs(expected);
        assertThat(path).hasValue("C:/oci/config");
        assertThat(profile).hasValue("VISTORIA");
    }

    @Test
    void deveConstruirProviderDeInstancePrincipalSemLerArquivo() {
        AbstractAuthenticationDetailsProvider expected = mock(AbstractAuthenticationDetailsProvider.class);
        OciAuthenticationProviderFactory factory = new OciAuthenticationProviderFactory(
                (path, profile) -> {
                    throw new AssertionError("O arquivo não deveria ser lido.");
                },
                () -> expected);
        OciGenAiProperties properties = propriedadesValidas();
        properties.setAuthMode(OciGenAiProperties.AuthMode.INSTANCE_PRINCIPAL);

        assertThat(factory.criar(properties)).isSameAs(expected);
    }

    @Test
    void deveTraduzirFalhaAoLerConfigFileSemExporCredencial() {
        OciAuthenticationProviderFactory factory = new OciAuthenticationProviderFactory(
                (path, profile) -> {
                    throw new IOException("conteúdo sensível");
                },
                () -> mock(AbstractAuthenticationDetailsProvider.class));

        assertThatThrownBy(() -> factory.criar(propriedadesValidas()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Não foi possível carregar a autenticação OCI pelo config file.")
                .hasMessageNotContaining("conteúdo sensível");
    }

    @Test
    void deveRepetirQuandoOciResponderComLimiteDeRequisicoes() {
        RetryConfiguration retry = configuracao().retryConfiguration(propriedadesValidas());

        assertThat(retry.getRetryCondition().shouldBeRetried(excecaoOci(429, false))).isTrue();
    }

    @Test
    void deveRepetirErroRecuperavelDoServidor() {
        RetryConfiguration retry = configuracao().retryConfiguration(propriedadesValidas());

        assertThat(retry.getRetryCondition().shouldBeRetried(excecaoOci(503, false))).isTrue();
    }

    @Test
    void deveRepetirTimeoutDoSdk() {
        RetryConfiguration retry = configuracao().retryConfiguration(propriedadesValidas());

        assertThat(retry.getRetryCondition().shouldBeRetried(excecaoOci(0, true))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void naoDeveRepetirErroNaoRecuperavel(int status) {
        RetryConfiguration retry = configuracao().retryConfiguration(propriedadesValidas());

        assertThat(retry.getRetryCondition().shouldBeRetried(excecaoOci(status, false))).isFalse();
    }

    @Test
    void deveAplicarTimeoutsETentativasLimitadasAoCliente() {
        OciGenAiProperties properties = propriedadesValidas();
        properties.setConnectTimeoutMs(1_500);
        properties.setReadTimeoutMs(9_000);
        properties.setMaxAttempts(3);

        RetryConfiguration retry = configuracao().retryConfiguration(properties);
        ClientConfiguration client = configuracao().clientConfiguration(properties, retry);

        assertThat(client.getConnectionTimeoutMillis()).isEqualTo(1_500);
        assertThat(client.getReadTimeoutMillis()).isEqualTo(9_000);
        assertThat(client.getRetryConfiguration()).isSameAs(retry);
        assertThat(retry.getTerminationStrategy().toString()).contains("3");
    }

    @Test
    void deveVerificarConfiguracaoSemPossuirClienteOuFazerChamada() {
        OciGenAiConfigCheck check = new OciGenAiConfigCheck(propriedadesValidas());

        OciGenAiConfigCheck.Resultado result = check.verificar();

        assertThat(result.configurado()).isTrue();
        assertThat(result.region()).isEqualTo("sa-saopaulo-1");
        assertThat(result.modelo()).isEqualTo("google.gemini-2.5-flash");
        assertThat(result.modoAutenticacao()).isEqualTo("config_file");
    }

    @Test
    void naoDeveCriarBeansOciQuandoOutroProviderEstiverAtivo() {
        new ApplicationContextRunner()
                .withUserConfiguration(OciGenAiConfiguration.class)
                .withPropertyValues("app.ia.provider=mock")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(OciGenAiConfigCheck.class);
                });
    }

    @Test
    void deveFalharAntesDeAutenticarQuandoCompartmentNaoForInformado() {
        new ApplicationContextRunner()
                .withUserConfiguration(OciGenAiConfiguration.class)
                .withPropertyValues(
                        "app.ia.provider=oci",
                        "oci.genai.compartment-id=",
                        "oci.genai.auth-mode=config_file")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "OCI_COMPARTMENT_ID é obrigatório para o provider OCI.");
                });
    }

    private OciGenAiConfiguration configuracao() {
        return new OciGenAiConfiguration();
    }

    private OciGenAiProperties propriedadesValidas() {
        OciGenAiProperties properties = new OciGenAiProperties();
        properties.setCompartmentId("ocid1.compartment.oc1..teste");
        return properties;
    }

    private BmcException excecaoOci(int status, boolean timeout) {
        BmcException exception = mock(BmcException.class);
        when(exception.getStatusCode()).thenReturn(status);
        when(exception.isTimeout()).thenReturn(timeout);
        return exception;
    }
}
