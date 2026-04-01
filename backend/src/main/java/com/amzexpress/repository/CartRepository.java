package com.amzexpress.repository;

import com.amzexpress.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserId(Long userId);
    long countByUserId(Long userId);
}