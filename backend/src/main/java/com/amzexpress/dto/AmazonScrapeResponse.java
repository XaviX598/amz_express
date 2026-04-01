package com.amzexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmazonScrapeResponse {
    private boolean success;
    private ProductInfo data;
    private String error;
    
    public static AmazonScrapeResponse success(ProductInfo data) {
        return AmazonScrapeResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
    
    public static AmazonScrapeResponse failure(String error) {
        return AmazonScrapeResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}
