package com.amzexpress.controller;

import com.amzexpress.entity.Address;
import com.amzexpress.entity.User;
import com.amzexpress.repository.UserRepository;
import com.amzexpress.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    
    private final AddressService addressService;
    private final UserRepository userRepository;
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
    
    @GetMapping
    public ResponseEntity<List<Address>> getAllAddresses(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(addressService.getAddressesByUser(user));
    }
    
    @GetMapping("/shipping")
    public ResponseEntity<List<Address>> getShippingAddresses(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(addressService.getShippingAddresses(user));
    }
    
    @GetMapping("/billing")
    public ResponseEntity<List<Address>> getBillingAddresses(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(addressService.getBillingAddresses(user));
    }
    
    @GetMapping("/default-shipping")
    public ResponseEntity<Address> getDefaultShippingAddress(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Address address = addressService.getDefaultShippingAddress(user);
        return address != null ? ResponseEntity.ok(address) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<Address> createAddress(
            Authentication authentication,
            @RequestBody AddressRequest request) {
        User user = getUserFromAuth(authentication);
        Address address = Address.builder()
                .addressType(request.addressType())
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .phone(request.phone())
                .isDefault(request.isDefault())
                .build();
        Address saved = addressService.saveAddress(user, address);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody AddressRequest request) {
        User user = getUserFromAuth(authentication);
        Address existing = addressService.getAddressById(user, id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        
        existing.setAddressType(request.addressType());
        existing.setStreet(request.street());
        existing.setCity(request.city());
        existing.setState(request.state());
        existing.setPostalCode(request.postalCode());
        existing.setCountry(request.country());
        existing.setPhone(request.phone());
        existing.setIsDefault(request.isDefault());
        
        Address saved = addressService.saveAddress(user, existing);
        return ResponseEntity.ok(saved);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable Long id) {
        User user = getUserFromAuth(authentication);
        addressService.deleteAddress(user, id);
        return ResponseEntity.ok().build();
    }
    
    public record AddressRequest(
            String addressType,
            String street,
            String city,
            String state,
            String postalCode,
            String country,
            String phone,
            Boolean isDefault
    ) {}
}