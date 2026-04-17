package com.agent.secretary.application.port.out;

import com.agent.secretary.domain.model.Task;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 외부 Task 저장소(Google Tasks 등)와 연동하기 위한 출력 포트
 */
public interface TaskOutboundPort {
    /**
     * 오늘 마감인 할 일 목록을 가져옵니다.
     */
    CompletableFuture<List<Task>> getTodaysTasks();

    /**
     * 내일 마감인 할 일 목록을 가져옵니다 (KST 기준).
     */
    CompletableFuture<List<Task>> getTomorrowsTasks();

    /**
     * 새로운 할 일을 저장합니다.
     */
    CompletableFuture<Task> saveTask(Task task);

    /**
     * ID로 특정 할 일을 조회합니다.
     */
    CompletableFuture<Task> getTaskById(String taskId);

    /**
     * 할 일의 제목/메모를 업데이트합니다.
     */
    CompletableFuture<Task> updateTask(Task task);

    /**
     * 할 일의 완료 상태를 업데이트합니다.
     */
    CompletableFuture<Void> updateTaskStatus(String taskId, boolean completed);
}
