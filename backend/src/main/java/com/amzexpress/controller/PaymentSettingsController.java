package com.amzexpress.controller;

import com.amzexpress.service.PaymentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentSettingsController {

    private final PaymentSettingsService paymentSettingsService;

    @GetMapping("/settings")
    public ResponseEntity<PaymentSettingsService.PaymentSettings> getPaymentSettings() {
        return ResponseEntity.ok(paymentSettingsService.getPaymentSettings());
    }
}
