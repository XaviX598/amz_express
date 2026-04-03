package com.amzexpress.dto;

import com.amzexpress.entity.OrderStatus;
import com.amzexpress.entity.PaymentMethod;
import com.amzexpress.entity.ShippingOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    private String productName;
    
    private String productAsin;
    
    @NotNull(message = "El precio del producto es requerido")
    private BigDecimal productPrice;
    
    private BigDecimal weight;
    
    @NotNull(message = "La opción de envío es requerida")
    private ShippingOption shippingOption;
    
    @NotNull(message = "El método de pago es requerido")
    private PaymentMethod paymentMethod;
    
    private String paymentReference;

    @NotBlank(message = "El código de seguridad es obligatorio")
    private String securityCode;
    
    private String amazonUrl;
    
    private String notes;
}
