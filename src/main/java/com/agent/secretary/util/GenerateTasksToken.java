package com.agent.secretary.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.tasks.TasksScopes;

import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Google Tasks OAuth2 토큰 최초 발급용 유틸리티.
 * 한 번만 실행하면 tokens/ 폴더에 인증 정보가 저장됩니다.
 * 실행: main() 메서드를 직접 Run
 */
public class GenerateTasksToken {

    public static void main(String[] args) throws Exception {
        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                jsonFactory, new FileReader("client_secret.json"));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets,
                List.of(TasksScopes.TASKS))
                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");

        System.out.println("✅ 토큰 발급 완료!");
        System.out.println("   Access Token: " + credential.getAccessToken());
        System.out.println("   tokens/ 폴더에 저장되었습니다.");
    }
}
