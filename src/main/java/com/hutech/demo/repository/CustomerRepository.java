package com.hutech.demo.repository;

import com.hutech.demo.model.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Customer> findWithLockByPhone(String phone);
}
