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
public class PricingCalculationRequest {
    private BigDecimal productPrice;
    private BigDecimal weight;
    private String shippingOption;
}
