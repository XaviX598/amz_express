package com.amzexpress.controller;

import com.amzexpress.dto.CreateOrderRequest;
import com.amzexpress.dto.OrderResponse;
import com.amzexpress.dto.UpdateOrderStatusRequest;
import com.amzexpress.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.createOrder(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrders(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderById(id, authentication.getName()));
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request, authentication.getName()));
    }

    @PostMapping(value = "/transfer-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitTransferProof(
            @RequestParam List<Long> orderIds,
            @RequestParam String bankCode,
            @RequestParam String bankName,
            @RequestParam String accountNumber,
            @RequestParam String accountHolder,
            @RequestParam MultipartFile proofFile,
            Authentication authentication) {
        orderService.submitTransferProof(
                authentication.getName(),
                orderIds,
                bankCode,
                bankName,
                accountNumber,
                accountHolder,
                proofFile
        );
        return ResponseEntity.ok(Map.of("message", "Comprobante recibido. Estado: comprobando pago."));
    }
}
