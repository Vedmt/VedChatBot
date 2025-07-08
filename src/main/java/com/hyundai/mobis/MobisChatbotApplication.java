package com.hyundai.mobis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableCaching
@EnableWebSocket
public class MobisChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobisChatbotApplication.class, args);
    }
} 