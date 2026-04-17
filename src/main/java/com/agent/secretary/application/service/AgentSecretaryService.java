package com.agent.secretary.application.service;

import com.agent.secretary.application.port.out.CalendarPort;
import com.agent.secretary.application.port.out.HealthPort;
import com.agent.secretary.application.port.out.TaskOutboundPort;
import com.agent.secretary.domain.model.FocusContext;
import com.agent.secretary.domain.model.Task;
import com.agent.secretary.domain.service.FocusEngine;
import com.agent.secretary.interfaces.slack.SlackBlockKitGenerator;
import com.slack.api.bolt.App;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final TaskOutboundPort taskPort;
    private final FocusEngine focusEngine = new FocusEngine();
    private final SlackBlockKitGenerator blockKitGenerator = new SlackBlockKitGenerator();
    private final App slackApp;

    @Value("${slack.channel-id}")
    private String channelId;

    /**
     * 1분마다 오늘의 Task 목록을 Slack에 전송합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    public void scheduledTaskBriefing() {
        log.info("Starting scheduled task briefing...");
        taskPort.getTodaysTasks().thenAccept(tasks -> {
            var blocks = blockKitGenerator.generateDailyBriefing(tasks, java.time.LocalDate.now());
            try {
                slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .blocks(blocks)
                        .text("오늘의 할 일 목록입니다.")
                );
                log.info("Successfully sent task briefing to Slack. tasks={}", tasks.size());
            } catch (Exception e) {
                log.error("Failed to send task briefing to Slack", e);
            }
        }).exceptionally(e -> {
            log.error("Failed to fetch tasks for briefing", e);
            return null;
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
                    
                    return new FocusAnalysisResult(score, insight, events);
                })
                .exceptionally(e -> {
                    log.error("Critical error during focus analysis", e);
                    return new FocusAnalysisResult(0, "오류가 발생했습니다. 로그를 확인하세요.", java.util.Collections.emptyList());
                });
    }

    public CompletableFuture<List<Task>> getTodaysTasks() {
        return taskPort.getTodaysTasks();
    }

    public CompletableFuture<Void> updateTaskStatus(String taskId, boolean completed) {
        return taskPort.updateTaskStatus(taskId, completed);
    }

    public record FocusAnalysisResult(int score, String insight, java.util.List<com.agent.secretary.domain.model.CalendarEvent> events) {}
}
