package com.agent.secretary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentSecretaryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentSecretaryApplication.class, args);
    }
}
