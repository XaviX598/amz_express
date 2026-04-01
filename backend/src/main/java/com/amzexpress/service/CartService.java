package com.amzexpress.service;

import com.amzexpress.entity.Cart;
import com.amzexpress.entity.User;
import com.amzexpress.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    
    public List<Cart> getCartByUser(User user) {
        return cartRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
    
    @Transactional
    public void addItemToCart(User user, String productName, double productPrice, double weight, String source) {
        Cart cartItem = Cart.builder()
                .user(user)
                .productName(productName)
                .productPrice(productPrice)
                .weight(weight)
                .source(source)
                .build();
        cartRepository.save(cartItem);
        log.info("Added item to cart for user: {}", user.getEmail());
    }
    
    @Transactional
    public void removeItemFromCart(User user, Long cartId) {
        cartRepository.findById(cartId).ifPresent(item -> {
            if (item.getUser().getId().equals(user.getId())) {
                cartRepository.delete(item);
                log.info("Removed item {} from cart for user: {}", cartId, user.getEmail());
            }
        });
    }
    
    @Transactional
    public void clearCart(User user) {
        cartRepository.deleteByUserId(user.getId());
        log.info("Cleared cart for user: {}", user.getEmail());
    }
    
    @Transactional
    public void saveCartItems(User user, List<Cart> items) {
        // Clear existing cart first
        cartRepository.deleteByUserId(user.getId());
        
        // Save new items
        for (Cart item : items) {
            Cart newItem = Cart.builder()
                    .user(user)
                    .productName(item.getProductName())
                    .productPrice(item.getProductPrice())
                    .weight(item.getWeight())
                    .source(item.getSource())
                    .build();
            cartRepository.save(newItem);
        }
        log.info("Saved {} cart items for user: {}", items.size(), user.getEmail());
    }
}