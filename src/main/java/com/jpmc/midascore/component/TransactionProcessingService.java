package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TransactionProcessingService {

    private final UserRepository userRepo;
    private final TransactionRecordRepository txRepo;

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
        // --- map users by *name* because UserRecord has `name` ---
        Optional<UserRecord> sOpt = userRepo.findById(tx.getSenderId());
        Optional<UserRecord> rOpt = userRepo.findById(tx.getRecipientId());
        // <— change if your getter differs
        if (sOpt.isEmpty() || rOpt.isEmpty()) {
            return; // invalid IDs -> discard
        }
        UserRecord sender = sOpt.get();
        UserRecord recipient = rOpt.get();

        // amount is float in UserRecord; cast if DTO provides double
        float amount = (float) tx.getAmount(); // <— if tx.getAmount() returns float already, this cast is harmless
        if (amount <= 0f) {
            return;
        }

        // sufficient funds?
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

        userRepo.findByName("waldorf").ifPresent(u
                -> System.out.println("__WALDORF_FINAL_FLOOR__=" + (int) Math.floor(u.getBalance()))
        );
    }
}
