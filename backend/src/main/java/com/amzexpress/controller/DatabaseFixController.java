package com.amzexpress.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DatabaseFixController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseFixController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/fix-orders-table")
    public Map<String, String> fixOrdersTable() {
        try {
            // Fix orders table
            jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check");
            jdbcTemplate.execute("ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN (" +
                "'PENDIENTE_PAGO', " +
                "'PAGADO', " +
                "'PEDIDO_REALIZADO', " +
                "'LLEGO_BODEGA_USA', " +
                "'EN_TRANSITO_ECUADOR', " +
                "'LLEGO_BODEGA_ECUADOR', " +
                "'LISTO_ENTREGA', " +
                "'ENTREGADO', " +
                "'CANCELADO'" +
            "))");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255)");
            
            // Fix order_status_history table
            jdbcTemplate.execute("ALTER TABLE order_status_history DROP CONSTRAINT IF EXISTS order_status_history_status_check");
            jdbcTemplate.execute("ALTER TABLE order_status_history ADD CONSTRAINT order_status_history_status_check CHECK (status IN (" +
                "'PENDIENTE_PAGO', " +
                "'PAGADO', " +
                "'PEDIDO_REALIZADO', " +
                "'LLEGO_BODEGA_USA', " +
                "'EN_TRANSITO_ECUADOR', " +
                "'LLEGO_BODEGA_ECUADOR', " +
                "'LISTO_ENTREGA', " +
                "'ENTREGADO', " +
                "'CANCELADO'" +
            "))");
            
            return Map.of("status", "success", "message", "All tables fixed");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
