package com.amzexpress.entity;

public enum ShippingOption {
    PICKUP("Retiro en bodega (Gratis)"),
    UBER("Delivery Uber (Solo Quito)"),
    SERVIENTREGA("Servientrega (Nacional)");

    private final String description;

    ShippingOption(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
