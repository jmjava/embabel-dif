package com.embabel.dif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Embabel 1.5 auto-configures from the starter — {@code @EnableAgents} is not required.
 */
@SpringBootApplication
public class DifApplication {

    public static void main(String[] args) {
        SpringApplication.run(DifApplication.class, args);
    }
}
