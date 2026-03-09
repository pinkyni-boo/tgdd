package com.hutech.demo.repository;
import com.hutech.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerName(String customerName);
    List<Order> findByPhoneOrderByCreatedAtDesc(String phone);
    Optional<Order> findByIdAndPhone(Long id, String phone);
}
