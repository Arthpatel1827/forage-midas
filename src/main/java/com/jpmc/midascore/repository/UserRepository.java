package com.jpmc.midascore.repository;

import java.util.Optional;

import com.jpmc.midascore.entity.UserRecord;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserRecord, Long> {
    Optional<UserRecord> findByName(String name);
    Optional<UserRecord> findByNameIgnoreCase(String name);
}
