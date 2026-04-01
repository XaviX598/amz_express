package com.amzexpress.repository;

import com.amzexpress.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Address> findByUserIdAndAddressType(Long userId, String addressType);
    Optional<Address> findByUserIdAndIsDefaultTrueAndAddressType(Long userId, String addressType);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}