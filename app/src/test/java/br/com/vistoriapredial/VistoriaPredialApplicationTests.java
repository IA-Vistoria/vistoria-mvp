package br.com.vistoriapredial;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VistoriaPredialApplicationTests {

    @Test
    void contextLoads() {
        // Gate de compilação: verifica que o contexto da aplicação sobe sem erros
        // @SpringBootTest ensures that the Spring ApplicationContext starts successfully
    }
}
