package br.com.vistoriapredial.integration.oci.genai.config;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;

import java.io.IOException;

public class OciAuthenticationProviderFactory {

    private final ConfigFileProviderLoader configFileProviderLoader;
    private final InstancePrincipalProviderLoader instancePrincipalProviderLoader;

    public OciAuthenticationProviderFactory() {
        this(
                ConfigFileAuthenticationDetailsProvider::new,
                () -> InstancePrincipalsAuthenticationDetailsProvider.builder().build());
    }

    OciAuthenticationProviderFactory(
            ConfigFileProviderLoader configFileProviderLoader,
            InstancePrincipalProviderLoader instancePrincipalProviderLoader) {
        this.configFileProviderLoader = configFileProviderLoader;
        this.instancePrincipalProviderLoader = instancePrincipalProviderLoader;
    }

    public AbstractAuthenticationDetailsProvider criar(OciGenAiProperties properties) {
        properties.validarParaUso();
        if (properties.getAuthMode() == OciGenAiProperties.AuthMode.INSTANCE_PRINCIPAL) {
            return instancePrincipalProviderLoader.load();
        }
        try {
            return configFileProviderLoader.load(
                    properties.getConfigFile(), properties.getConfigProfile());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível carregar a autenticação OCI pelo config file.", exception);
        }
    }

    @FunctionalInterface
    interface ConfigFileProviderLoader {
        AbstractAuthenticationDetailsProvider load(String path, String profile) throws IOException;
    }

    @FunctionalInterface
    interface InstancePrincipalProviderLoader {
        AbstractAuthenticationDetailsProvider load();
    }
}
