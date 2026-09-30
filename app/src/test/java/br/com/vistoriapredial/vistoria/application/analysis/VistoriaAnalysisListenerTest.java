package br.com.vistoriapredial.vistoria.application.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
class VistoriaAnalysisListenerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private VistoriaAnalysisProcessor processor;

    @Test
    void shouldProcessOnlyAfterPublishingTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(new VistoriaSubmetidaEvent(10L));
            verify(processor, never()).process(10L);
        });

        verify(processor, timeout(2_000)).process(10L);
    }
}
