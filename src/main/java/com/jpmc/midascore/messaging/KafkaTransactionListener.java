package com.jpmc.midascore.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.jpmc.midascore.foundation.Transaction;

@Component
public class KafkaTransactionListener {

    @KafkaListener(
        topics = "${general.kafka-topic}",
        groupId = "midas-core",
        containerFactory = "transactionKafkaListenerContainerFactory"
    )
        public void onMessage(Transaction tx) {
            System.out.println("AMOUNT=" + tx.getAmount());
        }
}
