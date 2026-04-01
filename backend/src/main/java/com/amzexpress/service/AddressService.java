package com.amzexpress.service;

import com.amzexpress.entity.Address;
import com.amzexpress.entity.User;
import com.amzexpress.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {
    
    private final AddressRepository addressRepository;
    
    public List<Address> getAddressesByUser(User user) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
    
    public List<Address> getShippingAddresses(User user) {
        return addressRepository.findByUserIdAndAddressType(user.getId(), "SHIPPING");
    }
    
    public List<Address> getBillingAddresses(User user) {
        return addressRepository.findByUserIdAndAddressType(user.getId(), "BILLING");
    }
    
    public Address getDefaultShippingAddress(User user) {
        return addressRepository.findByUserIdAndIsDefaultTrueAndAddressType(user.getId(), "SHIPPING")
                .orElse(null);
    }
    
    @Transactional
    public Address saveAddress(User user, Address address) {
        // If this is set as default, remove default from other addresses of same type
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> existing = addressRepository.findByUserIdAndAddressType(user.getId(), address.getAddressType());
            for (Address a : existing) {
                if (Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            }
        }
        
        address.setUser(user);
        Address saved = addressRepository.save(address);
        log.info("Saved address for user: {}", user.getEmail());
        return saved;
    }
    
    @Transactional
    public void deleteAddress(User user, Long addressId) {
        addressRepository.findById(addressId).ifPresent(address -> {
            if (address.getUser().getId().equals(user.getId())) {
                addressRepository.delete(address);
                log.info("Deleted address {} for user: {}", addressId, user.getEmail());
            }
        });
    }
    
    public Address getAddressById(User user, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, user.getId()).orElse(null);
    }
}