package com.agent.secretary.domain.model;

import java.time.LocalDateTime;

/**
 * 일정 도메인 엔터티 (Record)
 */
public record CalendarEvent(
    String summary,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}
