package br.com.vistoriapredial.vistoria.application.analysis;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VistoriaAnalysisListener {

    private final VistoriaAnalysisProcessor processor;

    public VistoriaAnalysisListener(VistoriaAnalysisProcessor processor) {
        this.processor = processor;
    }

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(VistoriaSubmetidaEvent event) {
        processor.process(event.vistoriaId());
    }
}
