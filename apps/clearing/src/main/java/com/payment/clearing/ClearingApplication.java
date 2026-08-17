package com.payment.clearing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.payment")
public class ClearingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearingApplication.class, args);
    }
}
