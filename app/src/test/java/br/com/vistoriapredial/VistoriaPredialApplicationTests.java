package br.com.vistoriapredial;

import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
import br.com.vistoriapredial.vistoria.application.MockIaIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VistoriaPredialApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private IaIntegrationService iaIntegrationService;

    @Test
    void contextLoads() {
        assertThat(iaIntegrationService).isInstanceOf(MockIaIntegrationService.class);
    }

    @Test
    void multipartRequestAllowsProtocolOverheadAboveSevenMegabytes() {
        assertThat(environment.getProperty("spring.servlet.multipart.max-file-size"))
                .isEqualTo("7MB");
        assertThat(environment.getProperty("spring.servlet.multipart.max-request-size"))
                .isEqualTo("8MB");
    }
}
