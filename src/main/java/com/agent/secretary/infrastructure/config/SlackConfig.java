package com.agent.secretary.infrastructure.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SlackConfig {

    @Value("${slack.bot-token}")
    private String botToken;

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Value("${slack.app-token}")
    private String appToken;

    @Bean
    public App initSlackApp() {
        AppConfig config = AppConfig.builder()
                .singleTeamBotToken(botToken)
                .signingSecret(signingSecret)
                .build();
        return new App(config);
    }

    @Bean
    public SocketModeApp socketModeApp(App app) throws Exception {
        SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
        // 소켓 모드 앱 비동기 시작
        socketModeApp.startAsync();
        log.info("Slack Socket Mode App has been initialized and started asynchronously.");
        return socketModeApp;
    }
}
