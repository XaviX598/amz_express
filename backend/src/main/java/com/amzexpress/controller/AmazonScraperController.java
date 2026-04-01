package com.amzexpress.controller;

import com.amzexpress.dto.AmazonScrapeRequest;
import com.amzexpress.dto.AmazonScrapeResponse;
import com.amzexpress.dto.ProductInfo;
import com.amzexpress.service.AmazonScraperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/amazon")
@RequiredArgsConstructor
public class AmazonScraperController {

    private final AmazonScraperService scraperService;

    @PostMapping("/scrape")
    public ResponseEntity<AmazonScrapeResponse> scrapeUrl(@Valid @RequestBody AmazonScrapeRequest request) {
        try {
            ProductInfo productInfo = scraperService.scrapeProduct(request.getUrl());
            
            if (productInfo == null) {
                return ResponseEntity.badRequest()
                        .body(AmazonScrapeResponse.failure("No se pudo extraer información del producto"));
            }
            
            return ResponseEntity.ok(AmazonScrapeResponse.success(productInfo));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AmazonScrapeResponse.failure("Error al procesar la URL: " + e.getMessage()));
        }
    }
}
