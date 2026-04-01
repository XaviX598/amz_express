package com.amzexpress.dto;

import com.amzexpress.entity.OrderStatus;
import com.amzexpress.entity.PaymentMethod;
import com.amzexpress.entity.ShippingOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String productName;
    private String productAsin;
    private BigDecimal productPrice;
    private BigDecimal weight;
    private ShippingOption shippingOption;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private BigDecimal totalPrice;
    private String notes;
    private String amazonUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StatusHistoryItem> statusHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryItem {
        private Long id;
        private OrderStatus status;
        private LocalDateTime createdAt;
    }
}
