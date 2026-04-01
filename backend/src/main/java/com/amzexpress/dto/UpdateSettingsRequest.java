package com.amzexpress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequest {
    
    @DecimalMin(value = "0.0", message = "Tax rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Tax rate must be between 0 and 1")
    private String taxRate;
    
    @DecimalMin(value = "0.0", message = "Handling rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Handling rate must be between 0 and 1")
    private String handlingRate;
    
    @DecimalMin(value = "0.0", message = "Shipping rate must be non-negative")
    private String shippingRatePerLb;
    
    @DecimalMin(value = "0.0", message = "Max shipping must be non-negative")
    private String maxShipping;
    
    @DecimalMin(value = "0.0", message = "Customs fee must be non-negative")
    private String customsFee;
    
    @DecimalMin(value = "0.0", message = "Max weight must be non-negative")
    private String maxWeightForStandard;
    
    @DecimalMin(value = "0.0", message = "Max price must be non-negative")
    private String maxPriceForStandard;
    
    @Pattern(regexp = "^\\d{6,20}$", message = "Invalid WhatsApp number format")
    private String whatsappNumber;

    private String bankName;

    private String bankAccountName;

    private String bankAccountNumber;

    private String bankAccountType;

    private String paypalEmail;

    private String paypalClientId;

    private String payphoneMerchantId;

    private String payphoneLink;
    
    public Map<String, String> toSettingsMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (taxRate != null) map.put("TAX_RATE", taxRate);
        if (handlingRate != null) map.put("HANDLING_RATE", handlingRate);
        if (shippingRatePerLb != null) map.put("SHIPPING_RATE_PER_LB", shippingRatePerLb);
        if (maxShipping != null) map.put("MAX_SHIPPING", maxShipping);
        if (customsFee != null) map.put("CUSTOMS_FEE", customsFee);
        if (maxWeightForStandard != null) map.put("MAX_WEIGHT_FOR_STANDARD", maxWeightForStandard);
        if (maxPriceForStandard != null) map.put("MAX_PRICE_FOR_STANDARD", maxPriceForStandard);
        if (whatsappNumber != null) map.put("WHATSAPP_NUMBER", whatsappNumber);
        if (bankName != null) map.put("BANK_NAME", bankName);
        if (bankAccountName != null) map.put("BANK_ACCOUNT_NAME", bankAccountName);
        if (bankAccountNumber != null) map.put("BANK_ACCOUNT_NUMBER", bankAccountNumber);
        if (bankAccountType != null) map.put("BANK_ACCOUNT_TYPE", bankAccountType);
        if (paypalEmail != null) map.put("PAYPAL_EMAIL", paypalEmail);
        if (paypalClientId != null) map.put("PAYPAL_CLIENT_ID", paypalClientId);
        if (payphoneMerchantId != null) map.put("PAYPHONE_MERCHANT_ID", payphoneMerchantId);
        if (payphoneLink != null) map.put("PAYPHONE_LINK", payphoneLink);
        return map;
    }
}
