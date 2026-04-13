package com.agent.secretary.domain.model;

import java.time.OffsetDateTime;

/**
 * Task 도메인 엔터티 (Record)
 * 외부 라이브러리 의존성 없이 순수 도메인 로직을 담습니다.
 */
public record Task(
    String id,
    String title,
    String notes,
    boolean completed,
    OffsetDateTime dueAt,
    OffsetDateTime completedAt
) {
    /**
     * 새로운 할 일 생성을 위한 팩토리 메서드
     */
    public static Task createNew(String title, String notes, OffsetDateTime dueAt) {
        return new Task(null, title, notes, false, dueAt, null);
    }
}
