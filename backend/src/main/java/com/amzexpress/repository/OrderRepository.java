package com.amzexpress.repository;

import com.amzexpress.entity.Order;
import com.amzexpress.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Order> findAllByOrderByCreatedAtDesc();
    
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    long countByUserId(Long userId);
}
