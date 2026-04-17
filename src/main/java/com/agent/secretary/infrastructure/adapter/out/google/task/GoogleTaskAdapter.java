package com.agent.secretary.infrastructure.adapter.out.google.task;

import com.agent.secretary.application.port.out.TaskOutboundPort;
import com.agent.secretary.domain.model.Task;
import com.google.api.services.tasks.Tasks;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Google Tasks API를 연동하여 Task 관리를 수행하는 어댑터
 */
@Component
@Slf4j
public class GoogleTaskAdapter implements TaskOutboundPort {

    private final Tasks googleTasksClient;

    @Value("${google.tasks.task-list-id:@default}")
    private String taskListId;

    private String resolvedTaskListId;

    public GoogleTaskAdapter(Tasks googleTasksClient) {
        this.googleTasksClient = googleTasksClient;
    }

    @PostConstruct
    public void resolveTaskListId() {
        try {
            var taskLists = googleTasksClient.tasklists().list().execute();

            if (taskLists.getItems() == null || taskLists.getItems().isEmpty()) {
                log.warn("[Tasks] 조회된 태스크 목록이 없음. @default 사용");
                resolvedTaskListId = "@default";
                return;
            }

            // 전체 목록 항상 출력 (설정 확인용)
            log.info("[Tasks] 사용 가능한 태스크 목록:");
            taskLists.getItems().forEach(tl ->
                log.info("  title='{}' id='{}'", tl.getTitle(), tl.getId())
            );

            // 이름(대소문자 무시) 또는 ID로 매칭
            var matched = taskLists.getItems().stream()
                .filter(tl -> tl.getTitle().equalsIgnoreCase(taskListId) || tl.getId().equals(taskListId))
                .findFirst();

            if (matched.isPresent()) {
                resolvedTaskListId = matched.get().getId();
                log.info("[Tasks] '{}' -> id={} 로 확정", matched.get().getTitle(), resolvedTaskListId);
            } else {
                // 이름으로 찾지 못한 경우 첫 번째 목록 사용
                resolvedTaskListId = taskLists.getItems().get(0).getId();
                log.warn("[Tasks] '{}' 와 일치하는 목록 없음. 첫 번째 목록 사용: title='{}' id='{}'",
                    taskListId,
                    taskLists.getItems().get(0).getTitle(),
                    resolvedTaskListId);
            }
        } catch (Exception e) {
            log.error("[Tasks] 목록 조회 실패. @default 사용", e);
            resolvedTaskListId = "@default";
        }
    }

    @Override
    public CompletableFuture<List<Task>> getTodaysTasks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var taskList = googleTasksClient.tasks().list(resolvedTaskListId)
                    .setShowCompleted(true)
                    .setShowHidden(true)
                    .execute();

                log.info("[Tasks] API 응답 태스크 수: {}", taskList.getItems() == null ? 0 : taskList.getItems().size());

                if (taskList.getItems() == null) {
                    return Collections.emptyList();
                }

                return taskList.getItems().stream()
                    .filter(t -> !"completed".equals(t.getStatus()))
                    .map(TaskMapper::toDomain)
                    .toList();
            } catch (Exception e) {
                log.error("Google Tasks 조회 중 오류 발생", e);
                throw new RuntimeException("Google Tasks 조회 실패", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Task>> getTomorrowsTasks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var taskList = googleTasksClient.tasks().list(resolvedTaskListId)
                    .setShowCompleted(false)
                    .execute();

                if (taskList.getItems() == null) {
                    return Collections.emptyList();
                }

                java.time.LocalDate tomorrow = java.time.LocalDate.now(
                    java.time.ZoneId.of("Asia/Seoul")).plusDays(1);

                return taskList.getItems().stream()
                    .filter(t -> !"completed".equals(t.getStatus()))
                    .map(TaskMapper::toDomain)
                    .filter(t -> t.dueAt() != null &&
                        t.dueAt().atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                            .toLocalDate().equals(tomorrow))
                    .toList();
            } catch (Exception e) {
                log.error("Google Tasks 내일 일정 조회 중 오류 발생", e);
                throw new RuntimeException("Google Tasks 내일 일정 조회 실패", e);
            }
        });
    }

    @Override
    public CompletableFuture<Task> saveTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var googleTask = TaskMapper.toGoogleModel(task);
                var savedTask = googleTasksClient.tasks().insert(resolvedTaskListId, googleTask).execute();
                return TaskMapper.toDomain(savedTask);
            } catch (Exception e) {
                log.error("Google Task 저장 중 오류 발생", e);
                throw new RuntimeException("Google Task 생성 실패", e);
            }
        });
    }

    @Override
    public CompletableFuture<Task> getTaskById(String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var googleTask = googleTasksClient.tasks().get(resolvedTaskListId, taskId).execute();
                return TaskMapper.toDomain(googleTask);
            } catch (Exception e) {
                log.error("Google Task 조회 중 오류 발생. taskId={}", taskId, e);
                throw new RuntimeException("Google Task 조회 실패", e);
            }
        });
    }

    @Override
    public CompletableFuture<Task> updateTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var googleTask = googleTasksClient.tasks().get(resolvedTaskListId, task.id()).execute();
                googleTask.setTitle(task.title());
                googleTask.setNotes(task.notes());
                var updated = googleTasksClient.tasks().update(resolvedTaskListId, task.id(), googleTask).execute();
                return TaskMapper.toDomain(updated);
            } catch (Exception e) {
                log.error("Google Task 업데이트 중 오류 발생. taskId={}", task.id(), e);
                throw new RuntimeException("Google Task 업데이트 실패", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateTaskStatus(String taskId, boolean completed) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 기존 태스크 정보를 먼저 조회하여 상태만 변경
                var task = googleTasksClient.tasks().get(resolvedTaskListId, taskId).execute();
                task.setStatus(completed ? "completed" : "needsAction");
                googleTasksClient.tasks().update(resolvedTaskListId, taskId, task).execute();
            } catch (Exception e) {
                log.error("Google Task 상태 업데이트 중 오류 발생", e);
                throw new RuntimeException("Google Task 상태 업데이트 실패", e);
            }
        });
    }
}
