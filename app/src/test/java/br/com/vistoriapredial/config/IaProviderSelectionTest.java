package br.com.vistoriapredial.config;

import br.com.vistoriapredial.integration.vlm.VlmConfiguration;
import br.com.vistoriapredial.integration.vlm.VlmIntegrationService;
import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.vistoria.application.MockIaIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IaProviderSelectionTest {

    @Test
    void ambienteNormalUsaOciComoProviderPadrao() throws IOException {
        Properties application = carregarApplicationProperties();

        assertThat(application.getProperty("app.ia.provider"))
                .isEqualTo("${APP_IA_PROVIDER:oci}");
    }

    @Test
    void ambienteNormalNaoPermiteMockMesmoQuandoSolicitado() {
        contexto(MockProviderConfiguration.class)
                .withPropertyValues("app.ia.provider=mock")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(MockIaIntegrationService.class));
    }

    @Test
    void perfilTestPodeUsarMockDeterministico() {
        contexto(MockProviderConfiguration.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withPropertyValues("app.ia.provider=mock")
                .run(context -> assertThat(context)
                        .hasSingleBean(MockIaIntegrationService.class));
    }

    @Test
    void perfilDemoPodeUsarMockDeterministico() {
        contexto(MockProviderConfiguration.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("demo"))
                .withPropertyValues("app.ia.provider=mock")
                .run(context -> assertThat(context)
                        .hasSingleBean(MockIaIntegrationService.class));
    }

    @Test
    void vlmSomenteEAtivadoPorSelecaoExplicita() {
        contexto(VlmProviderConfiguration.class)
                .withPropertyValues(
                        "app.ia.provider=vlm",
                        "vlm.api-key=chave-de-teste",
                        "vlm.url=http://127.0.0.1:9")
                .run(context -> assertThat(context)
                        .hasSingleBean(VlmIntegrationService.class));
    }

    private ApplicationContextRunner contexto(Class<?> configuration) {
        return new ApplicationContextRunner().withUserConfiguration(configuration);
    }

    private Properties carregarApplicationProperties() throws IOException {
        Properties application = new Properties();
        Path path = Path.of("src", "main", "resources", "application.properties");
        try (InputStream resource = Files.newInputStream(path)) {
            application.load(resource);
        }
        return application;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MockIaIntegrationService.class)
    static class MockProviderConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @Import({VlmConfiguration.class, VlmIntegrationService.class})
    static class VlmProviderConfiguration {

        @Bean
        StorageService storageService() {
            return mock(StorageService.class);
        }
    }
}
