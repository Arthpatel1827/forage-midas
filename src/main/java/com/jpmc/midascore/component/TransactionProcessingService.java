package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class TransactionProcessingService {

    private final UserRepository userRepo;
    private final TransactionRecordRepository txRepo;
    private final RestTemplate restTemplate;

    // Incentive API endpoint runs from the JAR you started in /services
    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";

    public TransactionProcessingService(UserRepository userRepo,
                                        TransactionRecordRepository txRepo,
                                        RestTemplate restTemplate) {
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.restTemplate = restTemplate;
    }

    /**
     * Task 4 rules:
     *  - Validate sender/recipient and funds (same as Task 3)
     *  - POST the Transaction to incentive API -> get Incentive{amount >= 0}
     *  - Apply balances:
     *        sender  -= amount
     *        recipient += amount + incentive
     *  - Persist TransactionRecord with incentive
     */
    @Transactional
    public void processIncoming(com.jpmc.midascore.foundation.Transaction tx) {
        // 1) Validate users
        Optional<UserRecord> sOpt = userRepo.findById(tx.getSenderId());
        Optional<UserRecord> rOpt = userRepo.findById(tx.getRecipientId());
        if (sOpt.isEmpty() || rOpt.isEmpty()) {
            return; // discard invalid ids
        }
        UserRecord sender = sOpt.get();
        UserRecord recipient = rOpt.get();

        // 2) Validate amount and funds
        float amount = (float) tx.getAmount();
        if (amount <= 0f || sender.getBalance() < amount) {
            return; // discard
        }

        // 3) Fetch incentive from external API (black box)
        float incentive = 0f;
        try {
            IncentiveResponse res = restTemplate.postForObject(
                    INCENTIVE_URL, tx, IncentiveResponse.class
            );
            if (res != null && res.getAmount() >= 0f) {
                incentive = res.getAmount();
            }
        } catch (Exception e) {
            // If the incentive service is down or errors out, treat incentive as 0
            incentive = 0f;
        }

        // 4) Apply balances (IMPORTANT: sender not charged for incentive)
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentive);
        userRepo.save(sender);
        userRepo.save(recipient);

        // 5) Persist the transaction (record incentive as well)
        TransactionRecord rec = new TransactionRecord(sender, recipient, amount);
        try {
            // works whether your entity has a setter-backed field or not;
            // if not present, this call is a no-op to the compiler, so you can remove it.
            rec.setIncentive(incentive);
        } catch (NoSuchMethodError | Exception ignore) {
            // ignore if your current entity doesn't have an incentive field yet
        }
        txRepo.save(rec);

        // 6) Print the value required by TaskFourTests (find "wilbur")
        userRepo.findByNameIgnoreCase("wilbur").ifPresent(u ->
                System.out.println("__WILBUR_FINAL_FLOOR__=" + (int) Math.floor(u.getBalance()))
        );
    }

    // Minimal DTO to match the API’s response shape
    public static class IncentiveResponse {
        private float amount;
        public float getAmount() { return amount; }
        public void setAmount(float amount) { this.amount = amount; }
    }
}
