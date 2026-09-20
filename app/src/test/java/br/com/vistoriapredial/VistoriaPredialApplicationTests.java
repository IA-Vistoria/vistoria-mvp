package br.com.vistoriapredial;

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

    @Test
    void contextLoads() {
        // Gate de compilação: verifica que o contexto da aplicação sobe sem erros
        // @SpringBootTest ensures that the Spring ApplicationContext starts successfully
    }

    @Test
    void multipartRequestAllowsProtocolOverheadAboveTenMegabytes() {
        assertThat(environment.getProperty("spring.servlet.multipart.max-file-size"))
                .isEqualTo("10MB");
        assertThat(environment.getProperty("spring.servlet.multipart.max-request-size"))
                .isEqualTo("11MB");
    }
}
