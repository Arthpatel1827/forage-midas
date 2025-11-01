package com.jpmc.midascore.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.component.TransactionProcessingService;

@Component
public class KafkaTransactionListener {

    private final TransactionProcessingService processor;

    // Spring will auto-wire this
    public KafkaTransactionListener(TransactionProcessingService processor) {
        this.processor = processor;
    }

    @KafkaListener(
        topics = "${general.kafka-topic}",
        groupId = "midas-core",
        containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void onMessage(Transaction tx) {
        // optional debug
        System.out.println("AMOUNT=" + tx.getAmount());
        processor.processIncoming(tx);
    }
}
