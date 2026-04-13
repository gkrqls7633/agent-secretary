package com.agent.secretary.infrastructure.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleTasksConfig {

    private static final String APPLICATION_NAME = "Agent Secretary";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.calendar.credentials-path}")
    private String credentialsPath;

    @Value("${google.tasks.delegated-user}")
    private String delegatedUser;

    @Bean
    public Tasks googleTasksClient() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // 서비스 계정으로 실제 사용자 계정을 impersonate
        var credentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createDelegated(delegatedUser)
                .createScoped(Collections.singleton(TasksScopes.TASKS));

        return new Tasks.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
