package com.amzexpress.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSettingsService {

    private final CalculatorSettingsService settingsService;

    public PaymentSettingsService(CalculatorSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public PaymentSettings getPaymentSettings() {
        return PaymentSettings.builder()
                .bankName(settingsService.getSetting(CalculatorSettingsService.KEY_BANK_NAME, ""))
                .bankAccountName(settingsService.getSetting(CalculatorSettingsService.KEY_BANK_ACCOUNT_NAME, ""))
                .bankAccountNumber(settingsService.getSetting(CalculatorSettingsService.KEY_BANK_ACCOUNT_NUMBER, ""))
                .bankAccountType(settingsService.getSetting(CalculatorSettingsService.KEY_BANK_ACCOUNT_TYPE, ""))
                .paypalEmail(settingsService.getSetting(CalculatorSettingsService.KEY_PAYPAL_EMAIL, ""))
                .paypalClientId(settingsService.getSetting(CalculatorSettingsService.KEY_PAYPAL_CLIENT_ID, ""))
                .payphoneMerchantId(settingsService.getSetting(CalculatorSettingsService.KEY_PAYPHONE_MERCHANT_ID, ""))
                .payphoneLink(settingsService.getSetting(CalculatorSettingsService.KEY_PAYPHONE_LINK, ""))
                .whatsappNumber(settingsService.getSetting(CalculatorSettingsService.KEY_WHATSAPP_NUMBER, ""))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSettings {
        private String bankName;
        private String bankAccountName;
        private String bankAccountNumber;
        private String bankAccountType;
        private String paypalEmail;
        private String paypalClientId;
        private String payphoneMerchantId;
        private String payphoneLink;
        private String whatsappNumber;
    }
}
