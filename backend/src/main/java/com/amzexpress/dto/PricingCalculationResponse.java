package com.amzexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingCalculationResponse {
    private BigDecimal productPrice;
    private BigDecimal taxes; // 15%
    private BigDecimal handling; // 9.27%
    private BigDecimal customs; // $21 fixed
    private BigDecimal shippingCost;
    private BigDecimal totalPrice;
    private boolean categoryC; // >8 lbs or >$400
    private String categoryCMessage;
    private String shippingDescription;
}
