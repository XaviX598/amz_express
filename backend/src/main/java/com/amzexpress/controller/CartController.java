package com.amzexpress.controller;

import com.amzexpress.entity.Cart;
import com.amzexpress.entity.User;
import com.amzexpress.repository.UserRepository;
import com.amzexpress.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    private final UserRepository userRepository;
    
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @GetMapping
    public ResponseEntity<List<Cart>> getCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Cart> cartItems = cartService.getCartByUser(user);
        return ResponseEntity.ok(cartItems);
    }
    
    @PostMapping("/add")
    public ResponseEntity<Void> addItem(
            Authentication authentication,
            @RequestBody CartItemRequest request) {
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        cartService.addItemToCart(user, request.productName(), request.productPrice(), request.weight(), request.source());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> removeItem(
            Authentication authentication,
            @PathVariable Long cartId) {
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        cartService.removeItemFromCart(user, cartId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        cartService.clearCart(user);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/save-all")
    public ResponseEntity<Void> saveAllItems(
            Authentication authentication,
            @RequestBody List<CartItemRequest> items) {
        User user = getUserFromAuth(authentication);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Cart> cartItems = items.stream()
                .map(item -> Cart.builder()
                        .productName(item.productName())
                        .productPrice(item.productPrice())
                        .weight(item.weight())
                        .source(item.source())
                        .build())
                .toList();
        cartService.saveCartItems(user, cartItems);
        return ResponseEntity.ok().build();
    }
    
    public record CartItemRequest(
            String productName,
            Double productPrice,
            Double weight,
            String source
    ) {}
}