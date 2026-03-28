package com.meetr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetrApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetrApplication.class, args);
    }
}
