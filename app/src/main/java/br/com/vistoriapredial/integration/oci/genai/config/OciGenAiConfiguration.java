package br.com.vistoriapredial.integration.oci.genai.config;

import br.com.vistoriapredial.integration.oci.genai.OciGenAiChatClient;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.retrier.RetryConfiguration;
import com.oracle.bmc.waiter.FixedTimeDelayStrategy;
import com.oracle.bmc.waiter.MaxAttemptsTerminationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OciGenAiProperties.class)
@ConditionalOnProperty(prefix = "app.ia", name = "provider", havingValue = "oci")
public class OciGenAiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OciAuthenticationProviderFactory ociAuthenticationProviderFactory() {
        return new OciAuthenticationProviderFactory();
    }

    @Bean
    AbstractAuthenticationDetailsProvider ociAuthenticationDetailsProvider(
            OciGenAiProperties properties,
            OciAuthenticationProviderFactory providerFactory) {
        properties.validarParaUso();
        return providerFactory.criar(properties);
    }

    @Bean
    RetryConfiguration retryConfiguration(OciGenAiProperties properties) {
        properties.validarParaUso();
        return RetryConfiguration.builder()
                .terminationStrategy(new MaxAttemptsTerminationStrategy(properties.getMaxAttempts()))
                .delayStrategy(new FixedTimeDelayStrategy(properties.getRetryDelayMs()))
                .retryCondition(exception -> exception != null
                        && (exception.isTimeout()
                        || exception.getStatusCode() == 429
                        || exception.getStatusCode() >= 500))
                .build();
    }

    @Bean
    ClientConfiguration clientConfiguration(
            OciGenAiProperties properties,
            RetryConfiguration retryConfiguration) {
        properties.validarParaUso();
        return ClientConfiguration.builder()
                .connectionTimeoutMillis(properties.getConnectTimeoutMs())
                .readTimeoutMillis(properties.getReadTimeoutMs())
                .retryConfiguration(retryConfiguration)
                .build();
    }

    @Bean(destroyMethod = "close")
    GenerativeAiInferenceClient generativeAiInferenceClient(
            OciGenAiProperties properties,
            AbstractAuthenticationDetailsProvider authenticationDetailsProvider,
            ClientConfiguration clientConfiguration) {
        properties.validarParaUso();
        return GenerativeAiInferenceClient.builder()
                .region(properties.getRegion())
                .configuration(clientConfiguration)
                .build(authenticationDetailsProvider);
    }

    @Bean
    OciGenAiChatClient ociGenAiChatClient(GenerativeAiInferenceClient client) {
        return client::chat;
    }

    @Bean
    OciGenAiConfigCheck ociGenAiConfigCheck(OciGenAiProperties properties) {
        return new OciGenAiConfigCheck(properties);
    }
}
