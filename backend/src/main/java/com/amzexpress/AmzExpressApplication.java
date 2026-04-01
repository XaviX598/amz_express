package com.amzexpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AmzExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmzExpressApplication.class, args);
    }
}
