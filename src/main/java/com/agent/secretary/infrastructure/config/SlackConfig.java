package com.agent.secretary.infrastructure.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {

    @Value("${slack.bot-token}")
    private String botToken;

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Bean
    public App initSlackApp() {
        AppConfig config = AppConfig.builder()
                .singleTeamBotToken(botToken)
                .signingSecret(signingSecret)
                .build();
        return new App(config);
    }

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackAppServlet(App app) {
        return new ServletRegistrationBean<>(new SlackAppServlet(app), "/slack/events");
    }
}
