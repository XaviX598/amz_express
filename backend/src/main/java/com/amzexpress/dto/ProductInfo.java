package com.amzexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {
    private String title;
    private String price;
    private String asin;
    private String image;      // URL de la imagen del producto
    private Double weight;     // Peso en libras
    private String weightUnit; // "lb" por defecto
}
