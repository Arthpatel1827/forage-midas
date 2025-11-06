package com.jpmc.midascore.api;

import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
public class BalanceController {

    private final UserRepository userRepo;

    public BalanceController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam long userId) {
        return userRepo.findById(userId)
                .map(u -> new Balance(u.getBalance()))
                .orElse(new Balance(0f));
    }
}
