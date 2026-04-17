package com.agent.secretary.application.service;

import com.agent.secretary.application.port.out.CalendarPort;
import com.agent.secretary.application.port.out.HealthPort;
import com.agent.secretary.application.port.out.TaskOutboundPort;
import com.agent.secretary.domain.model.CalendarEvent;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private volatile String eveningCheckInTs;
    private final CopyOnWriteArrayList<Task> eveningCheckInTasks =
        new CopyOnWriteArrayList<>();

    /**
     * 10시 정각마다 오늘의 Task 목록을 Slack에 전송합니다.
     */
//    @Scheduled(cron = "0 0 10 * * *") //매일 10시
    @Scheduled(cron = "0 * * * * *") //1분
    public void sendMorningBriefing() {
        log.info("Starting morning briefing...");
        taskPort.getTodaysTasks().thenAccept(tasks -> {
            var blocks = blockKitGenerator.generateDailyBriefing(tasks, LocalDate.now());
            try {
                slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .blocks(blocks)
                        .text("오늘의 할 일 목록입니다.")
                );
                log.info("Morning briefing sent. tasks={}", tasks.size());
            } catch (Exception e) {
                log.error("Failed to send task briefing to Slack", e);
            }
        }).exceptionally(e -> {
            log.error("Failed to fetch tasks for briefing", e);
            return null;
        });
    }
    /**
     * 매일 22:00 미완료 태스크 체크인 메시지를 Slack에 전송합니다.
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void sendEveningCheckIn() {
        log.info("Starting evening check-in...");
        taskPort.getTodaysTasks().thenAccept(tasks -> {
            if (tasks.isEmpty()) {
                log.info("No incomplete tasks. Skipping evening check-in.");
                return;
            }
            eveningCheckInTasks.clear();
            eveningCheckInTasks.addAll(tasks);
            var blocks = blockKitGenerator.generateEveningCheckIn(tasks);
            try {
                var response = slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .blocks(blocks)
                        .text("오늘 하루 마무리 체크인입니다.")
                );
                eveningCheckInTs = response.getTs();
                log.info("Evening check-in sent. tasks={}", tasks.size());
            } catch (Exception e) {
                log.error("Failed to send evening check-in", e);
            }
        }).exceptionally(e -> {
            log.error("Failed to fetch tasks for evening check-in", e);
            return null;
        });
    }

    /**
     * 매일 22:05 내일 일정 미리보기 메시지를 Slack에 전송합니다.
     */
    @Scheduled(cron = "0 5 22 * * *")
    public void sendTomorrowPreview() {
        log.info("Starting tomorrow preview...");
        taskPort.getTomorrowsTasks().thenAccept(tasks -> {
            LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
            var blocks = blockKitGenerator.generateTomorrowPreview(tasks, tomorrow);
            try {
                slackApp.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .blocks(blocks)
                        .text("내일 할 일 미리보기입니다.")
                );
                log.info("Tomorrow preview sent. tasks={}", tasks.size());
            } catch (Exception e) {
                log.error("Failed to send tomorrow preview", e);
            }
        }).exceptionally(e -> {
            log.error("Failed to fetch tomorrow tasks", e);
            return null;
        });
    }

    /**
     * 체크인 태스크 완료 처리 후 업데이트된 Block Kit 반환.
     */
    public CompletableFuture<List<com.slack.api.model.block.LayoutBlock>> markEveningTaskComplete(String taskId) {
        return taskPort.updateTaskStatus(taskId, true).thenApply(v -> {
            eveningCheckInTasks.replaceAll(t -> t.id().equals(taskId)
                ? new Task(t.id(), t.title(), t.notes(), true, t.dueAt(), OffsetDateTime.now())
                : t);
            return blockKitGenerator.generateEveningCheckIn(eveningCheckInTasks);
        });
    }

    public String getEveningCheckInTs() {
        return eveningCheckInTs;
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

    public CompletableFuture<List<Task>> getTomorrowsTasks() {
        return taskPort.getTomorrowsTasks();
    }

    public CompletableFuture<Task> getTaskById(String taskId) {
        return taskPort.getTaskById(taskId);
    }

    public CompletableFuture<Task> saveTask(Task task) {
        return taskPort.saveTask(task);
    }

    public CompletableFuture<Task> updateTask(Task task) {
        return taskPort.updateTask(task);
    }

    public CompletableFuture<Void> updateTaskStatus(String taskId, boolean completed) {
        return taskPort.updateTaskStatus(taskId, completed);
    }

    public record FocusAnalysisResult(int score, String insight, List<CalendarEvent> events) {}
}
