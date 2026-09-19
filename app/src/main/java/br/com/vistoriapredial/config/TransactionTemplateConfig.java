package br.com.vistoriapredial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Disponibiliza {@link TransactionTemplate} para casos de uso que precisam
 * de mais de uma transação curta dentro do mesmo método — por exemplo,
 * persistir antes e depois de uma chamada de rede, sem segurar conexão de
 * banco durante o I/O externo.
 */
@Configuration
public class TransactionTemplateConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
