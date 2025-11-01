package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransactionProcessingService {

    private final UserRepository userRepo;
    private final TransactionRecordRepository txRepo;

    // counts how many Kafka transactions we have processed in Task 3
    private final AtomicInteger processed = new AtomicInteger(0);

    // Forage Task 3 sends 22 messages
    private static final int TOTAL_MESSAGES = 22;

    public TransactionProcessingService(UserRepository userRepo, TransactionRecordRepository txRepo) {
        this.userRepo = userRepo;
        this.txRepo = txRepo;
    }

    /**
     * Valid if: - sender exists - recipient exists - sender.balance >= amount
     * If valid: record TX + adjust both balances. Otherwise: discard (no DB
     * change).
     */
    @Transactional
    public void processIncoming(com.jpmc.midascore.foundation.Transaction tx) {
        // look up by ID (senderId/recipientId are longs in the DTO)
        Optional<UserRecord> sOpt = userRepo.findById(tx.getSenderId());
        Optional<UserRecord> rOpt = userRepo.findById(tx.getRecipientId());
        if (sOpt.isEmpty() || rOpt.isEmpty()) {
            return;
        }

        UserRecord sender = sOpt.get();
        UserRecord recipient = rOpt.get();

        float amount = (float) tx.getAmount();
        if (amount <= 0f) {
            return;
        }

        if (sender.getBalance() < amount) {
            return;
        }

        // apply debit/credit
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount);
        userRepo.save(sender);
        userRepo.save(recipient);

        // store a persistent record
        TransactionRecord rec = new TransactionRecord(sender, recipient, amount);
        txRepo.save(rec);

        // Print ONCE after the last message is processed
        userRepo.findByNameIgnoreCase("Waldorf").ifPresent(u
                -> System.out.println("__WALDORF_FINAL_FLOOR__=" + (int) Math.floor(u.getBalance()))
        );
    }
}
