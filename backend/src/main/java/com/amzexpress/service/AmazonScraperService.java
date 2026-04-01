package com.amzexpress.service;

import com.amzexpress.dto.ProductInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AmazonScraperService {

    private static final String SCRAPINGDOG_URL = "https://api.scrapingdog.com/amazon/product";
    private static final String EASYPARSER_URL = "https://api.easyparser.com/v1/amazon/package-dimensions";
    private static final String EASYPARSER_API_KEY = "demo"; // Free demo key
    private static final String ASINDATAAPI_URL = "https://api.asindataapi.com/request";
    private static final int TIMEOUT_SECONDS = 25;

    // Patterns for ASIN extraction
    private static final Pattern DP_ASIN_PATTERN = Pattern.compile("/dp/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE);
    private static final Pattern GP_ASIN_PATTERN = Pattern.compile("/gp/product/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE);
    private static final Pattern A_CO_PATTERN = Pattern.compile("/d/([a-zA-Z0-9]{10})", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String scrapingdogApiKey;
    private final String asinDataApiKey;

    public AmazonScraperService(
            @Value("${scrapingdog.api-key:}") String scrapingdogApiKey,
            @Value("${asindataapi.api-key:}") String asinDataApiKey) {
        this.scrapingdogApiKey = scrapingdogApiKey;
        this.asinDataApiKey = asinDataApiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        log.info("AmazonScraperService initialized. Scrapingdog API key: {}, ASIN Data API key: {}",
                scrapingdogApiKey != null && !scrapingdogApiKey.isBlank() ? "CONFIGURED" : "MISSING",
                asinDataApiKey != null && !asinDataApiKey.isBlank() ? "CONFIGURED" : "MISSING");
    }

    public ProductInfo scrapeProduct(String url) {
        try {
            if (!isValidAmazonUrl(url)) {
                log.warn("Invalid Amazon URL: {}", url);
                return null;
            }

            // Step 1: Resolve redirects for short URLs
            String resolvedUrl = resolveRedirectUrl(url);
            log.info("Original URL: {}, Resolved URL: {}", url, resolvedUrl);

            // Step 2: Extract ASIN
            String asin = extractAsin(resolvedUrl);
            if (asin == null) {
                log.error("Could not extract ASIN from URL: {}", resolvedUrl);
                return null;
            }
            log.info("Extracted ASIN: {} from URL: {}", asin, resolvedUrl);

            // Step 3: Call Scrapingdog
            JsonNode response = callScrapingdog(asin);
            
            // If Scrapingdog fails or doesn't return valid data, try ASIN Data API
            if (response == null) {
                log.info("Scrapingdog failed, trying ASIN Data API for ASIN: {}", asin);
                response = callAsinDataApi(asin);
            }
            
            if (response == null) {
                log.error("All APIs failed for ASIN: {}", asin);
                return null;
            }

            // Handle nested response structure (Scrapingdog returns data in 'result' array)
            JsonNode productData = response;
            if (response.has("result") && response.get("result").isArray()) {
                JsonNode firstResult = response.get("result").get(0);
                if (firstResult != null) {
                    productData = firstResult;
                    log.debug("Using nested product data from result[0]");
                }
            }

            // Step 4: Extract fields from JSON
            String title = productData.has("title") ? productData.get("title").asText() : null;
            String priceText = productData.has("price") ? productData.get("price").asText() : null;
            String image = extractFirstImage(productData);
            Double weight = extractWeight(productData);

            // If weight not found from Scrapingdog, try ASIN Data API
            if (weight == null) {
                log.info("Weight not found in Scrapingdog, trying ASIN Data API for ASIN: {}", asin);
                weight = fetchWeightFromAsinDataApi(asin);
            }

            if (title == null || title.isBlank()) {
                log.warn("Scrapingdog did not return a valid title for ASIN: {}", asin);
                return null;
            }

            log.info("Scrapingdog success - Title: {}, Price: {}, Image: {}, Weight: {}",
                    title, priceText, image, weight);

            return ProductInfo.builder()
                    .title(title)
                    .price(cleanPrice(priceText))
                    .image(image)
                    .weight(weight)
                    .weightUnit(weight != null ? "lb" : null)
                    .asin(asin)
                    .build();

        } catch (Exception e) {
            log.error("Error scraping Amazon URL: {}", url, e);
            return null;
        }
    }

    private JsonNode callScrapingdog(String asin) {
        if (scrapingdogApiKey == null || scrapingdogApiKey.isBlank()) {
            log.error("Scrapingdog API key not configured!");
            return null;
        }

        try {
            String apiUrl = SCRAPINGDOG_URL + "?domain=com&api_key=" + scrapingdogApiKey + "&asin=" + asin;
            log.info("Calling Scrapingdog: {}", apiUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Scrapingdog returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            return objectMapper.readTree(response.body());

        } catch (Exception e) {
            log.error("Error calling Scrapingdog API for ASIN: {}", asin, e);
            return null;
        }
    }

    private String extractFirstImage(JsonNode response) {
        try {
            if (response.has("images") && response.get("images").isArray()) {
                for (JsonNode img : response.get("images")) {
                    String url = img.asText();
                    if (url != null && !url.isBlank() && url.contains("amazon.com") && url.endsWith(".jpg")) {
                        // Use smaller thumbnail version
                        return url.replace("_SL1500_", "_SR38,50_");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract image: {}", e.getMessage());
        }
        return null;
    }

    private Double extractWeight(JsonNode response) {
        try {
            // Try different possible paths where Scrapingdog might return weight
            
            // Option 1: product_information object
            if (response.has("product_information")) {
                JsonNode productInfo = response.get("product_information");
                
                // Try common field names
                String[] weightFields = {
                    "Item Weight",
                    "Package Weight", 
                    "Shipping Weight",
                    "Weight",
                    "Item weight",
                    "Package weight",
                    "Shipping weight",
                    "weight"
                };
                
                for (String field : weightFields) {
                    if (productInfo.has(field)) {
                        Double parsed = parseWeight(productInfo.get(field).asText());
                        if (parsed != null) {
                            log.info("Found weight in product_information.{}: {}", field, parsed);
                            return parsed;
                        }
                    }
                }
            }
            
            // Option 2: Top-level weight field
            if (response.has("weight")) {
                Double parsed = parseWeight(response.get("weight").asText());
                if (parsed != null) {
                    log.info("Found weight in root: {}", parsed);
                    return parsed;
                }
            }
            
            // Option 3: product_details object
            if (response.has("product_details") && response.get("product_details").isObject()) {
                JsonNode details = response.get("product_details");
                JsonNode weightNode = details.get("weight");
                if (weightNode != null) {
                    Double parsed = parseWeight(weightNode.asText());
                    if (parsed != null) {
                        log.info("Found weight in product_details: {}", parsed);
                        return parsed;
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Could not extract weight: {}", e.getMessage());
        }
        log.info("No weight found (user will enter manually)");
        return null;
    }

    private Double parseWeight(String weightText) {
        if (weightText == null || weightText.isBlank()) {
            return null;
        }
        
        // Match: "4.73 pounds", "6.1 ounces", "4.73 lbs", "0.5 pounds", "1 Kilograms", "500 g"
        Pattern p = Pattern.compile("([\\d.]+)\\s*(pounds?|lbs?|ounces?|oz|kilograms?|kg|grams?|g)?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(weightText);
        
        if (m.find()) {
            double value = Double.parseDouble(m.group(1));
            String unit = m.group(2);
            
            // Convert to pounds
            if (unit != null) {
                if (unit.equalsIgnoreCase("oz") || unit.equalsIgnoreCase("ounce") || unit.equalsIgnoreCase("ounces")) {
                    value = value / 16.0;
                } else if (unit.equalsIgnoreCase("kg") || unit.equalsIgnoreCase("kilogram") || unit.equalsIgnoreCase("kilograms")) {
                    value = value * 2.20462; // Convert kg to pounds
                } else if (unit.equalsIgnoreCase("g") || unit.equalsIgnoreCase("gram") || unit.equalsIgnoreCase("grams")) {
                    value = value / 453.592; // Convert grams to pounds
                }
            }
            
            log.info("Parsed weight: {} lb from '{}'", value, weightText);
            return value;
        }
        
        return null;
    }

    private Double fetchWeightFromEasyParser(String asin) {
        try {
            // EasyParser package dimensions API - returns weight in pounds
            String apiUrl = EASYPARSER_URL + "?asin=" + asin + "&domain=com&api_key=" + EASYPARSER_API_KEY;
            log.info("Calling EasyParser for weight: {}", apiUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                
                // EasyParser returns: package_weight (in pounds), product_weight
                if (json.has("package_weight")) {
                    double weight = json.get("package_weight").asDouble();
                    log.info("EasyParser returned weight: {} lbs", weight);
                    return weight;
                }
                if (json.has("product_weight")) {
                    double weight = json.get("product_weight").asDouble();
                    log.info("EasyParser returned product_weight: {} lbs", weight);
                    return weight;
                }
            } else {
                log.warn("EasyParser returned status {}: {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.warn("Error calling EasyParser: {}", e.getMessage());
        }
        return null;
    }

    private String cleanPrice(String price) {
        if (price == null || price.isBlank()) {
            return null;
        }
        String cleaned = price.replaceAll("[^\\d.]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            Double.parseDouble(cleaned);
            return cleaned;
        } catch (NumberFormatException e) {
            return price;
        }
    }

    private boolean isValidAmazonUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return (lowerUrl.contains("amazon.") || lowerUrl.contains("a.co")) &&
               (lowerUrl.contains("/dp/") || lowerUrl.contains("/gp/product/") || lowerUrl.contains("/d/") || containsAsin(lowerUrl));
    }

    private boolean containsAsin(String url) {
        return Pattern.compile("[A-Z0-9]{10}", Pattern.CASE_INSENSITIVE).matcher(url).find();
    }

    private String resolveRedirectUrl(String url) {
        // Already has /dp/ - no need to resolve
        if (url.toLowerCase().contains("amazon.com") && url.toLowerCase().contains("/dp/")) {
            return url;
        }

        // Handle short URLs a.co/d/ASIN
        if (url.toLowerCase().contains("a.co/d/")) {
            try {
                // Use GET instead of HEAD to properly follow redirects
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
                String finalUrl = response.uri().toString();
                log.info("Resolved a.co short URL from {} to {}", url, finalUrl);
                return finalUrl;

            } catch (Exception e) {
                log.warn("Could not resolve short URL {}, using as-is: {}", url, e.getMessage());
                // Fallback: try to extract ASIN from the short URL directly
                return url;
            }
        }

        return url;
    }

    private String extractAsin(String url) {
        // 1. Standard /dp/ASIN format
        Matcher dpMatcher = DP_ASIN_PATTERN.matcher(url);
        if (dpMatcher.find()) {
            return dpMatcher.group(1).toUpperCase();
        }

        // 2. /gp/product/ASIN format
        Matcher gpMatcher = GP_ASIN_PATTERN.matcher(url);
        if (gpMatcher.find()) {
            return gpMatcher.group(1).toUpperCase();
        }

        // 3. Short URL a.co/d/ASIN format
        Matcher aCoMatcher = A_CO_PATTERN.matcher(url);
        if (aCoMatcher.find()) {
            String asin = aCoMatcher.group(1).toUpperCase();
            if (isValidAsin(asin)) {
                return asin;
            }
        }

        return null;
    }

    private boolean isValidAsin(String asin) {
        return asin != null && asin.matches("^[A-Z0-9]{10}$");
    }

    private JsonNode callAsinDataApi(String asin) {
        if (asinDataApiKey == null || asinDataApiKey.isBlank()) {
            log.warn("ASIN Data API key not configured!");
            return null;
        }

        try {
            String apiUrl = ASINDATAAPI_URL + "?api_key=" + asinDataApiKey + "&type=product&amazon_domain=amazon.com&asin=" + asin;
            log.info("Calling ASIN Data API: {}", apiUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("ASIN Data API returned status {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode json = objectMapper.readTree(response.body());
            
            // Check if request was successful
            if (json.has("request_info") && json.get("request_info").has("success")) {
                if (json.get("request_info").get("success").asBoolean()) {
                    log.info("ASIN Data API success for ASIN: {}", asin);
                    return json.get("product");
                }
            }
            
            log.warn("ASIN Data API request failed: {}", response.body());
            return null;

        } catch (Exception e) {
            log.error("Error calling ASIN Data API for ASIN: {}", asin, e);
            return null;
        }
    }

    private Double fetchWeightFromAsinDataApi(String asin) {
        if (asinDataApiKey == null || asinDataApiKey.isBlank()) {
            log.warn("ASIN Data API key not configured!");
            return null;
        }

        try {
            String apiUrl = ASINDATAAPI_URL + "?api_key=" + asinDataApiKey + "&type=product&amazon_domain=amazon.com&asin=" + asin;
            log.info("Fetching weight from ASIN Data API: {}", asin);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                
                // Check if request was successful
                if (json.has("request_info") && json.get("request_info").get("success").asBoolean()) {
                    JsonNode product = json.get("product");
                    
                    // Try to get weight from top-level weight field
                    if (product != null && product.has("weight")) {
                        String weightStr = product.get("weight").asText();
                        Double parsed = parseWeight(weightStr);
                        if (parsed != null) {
                            log.info("Found weight from ASIN Data API (top-level): {} lb", parsed);
                            return parsed;
                        }
                    }
                    
                    // Try to get from specifications
                    if (product != null && product.has("specifications")) {
                        JsonNode specs = product.get("specifications");
                        for (JsonNode spec : specs) {
                            if (spec.has("name") && spec.get("name").asText().equalsIgnoreCase("Item Weight")) {
                                String weightStr = spec.get("value").asText();
                                Double parsed = parseWeight(weightStr);
                                if (parsed != null) {
                                    log.info("Found weight from ASIN Data API (specifications): {} lb", parsed);
                                    return parsed;
                                }
                            }
                        }
                    }
                }
            }
            
            log.warn("ASIN Data API did not return weight for ASIN: {}", asin);
            return null;

        } catch (Exception e) {
            log.error("Error fetching weight from ASIN Data API for ASIN: {}", asin, e);
            return null;
        }
    }
}
