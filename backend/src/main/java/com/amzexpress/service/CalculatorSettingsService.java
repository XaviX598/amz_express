package com.amzexpress.service;

import com.amzexpress.entity.CalculatorSettings;
import com.amzexpress.repository.CalculatorSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CalculatorSettingsService {

    private final CalculatorSettingsRepository repository;
    
    // Simple in-memory cache
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    
    // Default settings
    public static final String KEY_TAX_RATE = "TAX_RATE";
    public static final String KEY_HANDLING_RATE = "HANDLING_RATE";
    public static final String KEY_SHIPPING_RATE_PER_LB = "SHIPPING_RATE_PER_LB";
    public static final String KEY_MAX_SHIPPING = "MAX_SHIPPING";
    public static final String KEY_CUSTOMS_FEE = "CUSTOMS_FEE";
    public static final String KEY_MAX_WEIGHT_FOR_STANDARD = "MAX_WEIGHT_FOR_STANDARD";
    public static final String KEY_MAX_PRICE_FOR_STANDARD = "MAX_PRICE_FOR_STANDARD";
    public static final String KEY_WHATSAPP_NUMBER = "WHATSAPP_NUMBER";
    
    // Payment settings
    public static final String KEY_BANK_NAME = "BANK_NAME";
    public static final String KEY_BANK_ACCOUNT_NAME = "BANK_ACCOUNT_NAME";
    public static final String KEY_BANK_ACCOUNT_NUMBER = "BANK_ACCOUNT_NUMBER";
    public static final String KEY_BANK_ACCOUNT_TYPE = "BANK_ACCOUNT_TYPE";
    public static final String KEY_PAYPAL_EMAIL = "PAYPAL_EMAIL";
    public static final String KEY_PAYPAL_CLIENT_ID = "PAYPAL_CLIENT_ID";
    public static final String KEY_PAYPHONE_MERCHANT_ID = "PAYPHONE_MERCHANT_ID";
    public static final String KEY_PAYPHONE_LINK = "PAYPHONE_LINK";
    
    // Default values
    private static final Map<String, String> DEFAULT_SETTINGS = new HashMap<>();
    static {
        DEFAULT_SETTINGS.put(KEY_TAX_RATE, "0.15");
        DEFAULT_SETTINGS.put(KEY_HANDLING_RATE, "0.0927");
        DEFAULT_SETTINGS.put(KEY_SHIPPING_RATE_PER_LB, "5");
        DEFAULT_SETTINGS.put(KEY_MAX_SHIPPING, "40");
        DEFAULT_SETTINGS.put(KEY_CUSTOMS_FEE, "21");
        DEFAULT_SETTINGS.put(KEY_MAX_WEIGHT_FOR_STANDARD, "8");
        DEFAULT_SETTINGS.put(KEY_MAX_PRICE_FOR_STANDARD, "400");
        DEFAULT_SETTINGS.put(KEY_WHATSAPP_NUMBER, "593985295277");
        DEFAULT_SETTINGS.put(KEY_BANK_NAME, "Banco Pichincha");
        DEFAULT_SETTINGS.put(KEY_BANK_ACCOUNT_NAME, "Amz Express Ecuador");
        DEFAULT_SETTINGS.put(KEY_BANK_ACCOUNT_NUMBER, "1234567890");
        DEFAULT_SETTINGS.put(KEY_BANK_ACCOUNT_TYPE, "Ahorros");
        DEFAULT_SETTINGS.put(KEY_PAYPAL_EMAIL, "pagos@amzexpress.com");
        DEFAULT_SETTINGS.put(KEY_PAYPAL_CLIENT_ID, "");
        DEFAULT_SETTINGS.put(KEY_PAYPHONE_MERCHANT_ID, "");
        DEFAULT_SETTINGS.put(KEY_PAYPHONE_LINK, "");
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        List<CalculatorSettings> settings = repository.findAll();
        Map<String, String> result = new HashMap<>();
        
        // Start with defaults
        result.putAll(DEFAULT_SETTINGS);
        
        // Override with database values
        for (CalculatorSettings setting : settings) {
            result.put(setting.getSettingKey(), setting.getSettingValue());
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return getSetting(key, DEFAULT_SETTINGS.get(key));
    }
    
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        // Check cache first
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        
        // Get from database
        Optional<CalculatorSettings> setting = repository.findBySettingKey(key);
        String value = setting.map(CalculatorSettings::getSettingValue)
                .orElse(defaultValue);
        
        // Cache the value
        if (value != null) {
            cache.put(key, value);
        }
        
        return value;
    }
    
    public BigDecimal getTaxRate() {
        return new BigDecimal(getSetting(KEY_TAX_RATE));
    }
    
    public BigDecimal getHandlingRate() {
        return new BigDecimal(getSetting(KEY_HANDLING_RATE));
    }
    
    public BigDecimal getShippingRatePerLb() {
        return new BigDecimal(getSetting(KEY_SHIPPING_RATE_PER_LB));
    }
    
    public BigDecimal getMaxShipping() {
        return new BigDecimal(getSetting(KEY_MAX_SHIPPING));
    }
    
    public BigDecimal getCustomsFee() {
        return new BigDecimal(getSetting(KEY_CUSTOMS_FEE));
    }
    
    public BigDecimal getMaxWeightForStandard() {
        return new BigDecimal(getSetting(KEY_MAX_WEIGHT_FOR_STANDARD));
    }
    
    public BigDecimal getMaxPriceForStandard() {
        return new BigDecimal(getSetting(KEY_MAX_PRICE_FOR_STANDARD));
    }
    
    public String getWhatsappNumber() {
        return getSetting(KEY_WHATSAPP_NUMBER);
    }
    
    @Transactional
    public Map<String, String> updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            CalculatorSettings setting = repository.findBySettingKey(key)
                    .orElse(CalculatorSettings.builder()
                            .settingKey(key)
                            .settingValue(value)
                            .build());
            
            setting.setSettingValue(value);
            repository.save(setting);
            
            // Update cache
            cache.put(key, value);
        }
        
        return getAllSettings();
    }
    
    public void clearCache() {
        cache.clear();
    }
}
