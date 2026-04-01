package com.amzexpress.controller;

import com.amzexpress.dto.PricingCalculationRequest;
import com.amzexpress.dto.PricingCalculationResponse;
import com.amzexpress.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final OrderService orderService;

    @PostMapping("/calculate")
    public ResponseEntity<PricingCalculationResponse> calculatePrice(@RequestBody PricingCalculationRequest request) {
        return ResponseEntity.ok(orderService.calculatePricing(
                request.getProductPrice(),
                request.getWeight(),
                request.getShippingOption()
        ));
    }

    @GetMapping("/calculate")
    public ResponseEntity<PricingCalculationResponse> calculatePriceGet(
            @RequestParam BigDecimal productPrice,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) String shippingOption) {
        return ResponseEntity.ok(orderService.calculatePricing(productPrice, weight, shippingOption));
    }
}
