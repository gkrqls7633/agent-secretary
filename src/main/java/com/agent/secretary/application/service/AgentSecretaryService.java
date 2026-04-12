package com.agent.secretary.application.service;

import com.agent.secretary.application.port.out.CalendarPort;
import com.agent.secretary.application.port.out.HealthPort;
import com.agent.secretary.domain.model.FocusContext;
import com.agent.secretary.domain.service.FocusEngine;
import com.agent.secretary.interfaces.slack.SlackBlockKitGenerator;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 집중도 분석 및 스케줄링 로직을 담당하는 애플리케이션 서비스.
 * Virtual Threads (Project Loom) 환경에서 최적화된 비동기 처리 수행.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentSecretaryService {

    private final CalendarPort calendarPort;
    private final HealthPort healthPort;
    private final FocusEngine focusEngine = new FocusEngine();
    private final SlackBlockKitGenerator blockKitGenerator = new SlackBlockKitGenerator();
    private final App slackApp;

    @Value("${slack.channel-id}")
    private String channelId;

    /**
     * 매주 평일 오전 8시에 오늘의 브리핑을 생성합니다.
     */
//    @Scheduled(cron = "0 0 8 * * MON-FRI")
    @Scheduled(cron = "0 * * * * *")
    public void scheduledMorningBriefing() {
        log.info("Starting scheduled morning briefing analysis...");
        sendFocusNotification("오늘의 핵심 프로젝트 업무");
    }

    /**
     * 매주 평일 9시부터 18시까지 매 정각마다 업무 집중도를 체크합니다.
     */
    @Scheduled(cron = "0 0 9-18 * * MON-FRI")
    @Scheduled(cron = "0 * * * * *")
    public void hourlyFocusCheck() {
        log.info("Starting hourly focus check analysis...");
        sendFocusNotification("현재 진행 중인 업무");
    }

    private void sendFocusNotification(String taskName) {
        analyzeCurrentStatus().thenAccept(result -> {
            log.info("Focus Check Status: Focus Score is {}", result.score());

            var blocks = blockKitGenerator.generateFocusProposal(
                    result.score(),
                    result.insight(),
                    taskName
            );

            try {
                slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .blocks(blocks)
                );
                log.info("Successfully sent focus notification to Slack.");
            } catch (Exception e) {
                log.error("Failed to send focus notification to Slack", e);
            }
        });
    }
    /**
     * 현재 상태를 분석하여 집중도 점수와 인사이트를 반환합니다.
     */
    public CompletableFuture<FocusAnalysisResult> analyzeCurrentStatus() {
        var healthFuture = healthPort.getTodaysHealthMetrics();
        var calendarFuture = calendarPort.getTodaysEvents();

        return CompletableFuture.allOf(healthFuture, calendarFuture)
                .thenApply(v -> {
                    var health = healthFuture.join();
                    var events = calendarFuture.join();
                    
                    var context = new FocusContext(health, events);
                    int score = focusEngine.calculateFocusScore(context);
                    String insight = focusEngine.getFocusInsight(score);
                    
                    return new FocusAnalysisResult(score, insight);
                })
                .exceptionally(e -> {
                    log.error("Critical error during focus analysis", e);
                    return new FocusAnalysisResult(0, "오류가 발생했습니다. 로그를 확인하세요.");
                });
    }

    public record FocusAnalysisResult(int score, String insight) {}
}
