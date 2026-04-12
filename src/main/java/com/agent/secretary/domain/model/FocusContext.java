package com.agent.secretary.domain.model;

import java.util.List;

/**
 * 집중도 산출을 위한 맥락 정보 (Record)
 */
public record FocusContext(
    HealthData health,
    List<CalendarEvent> calendarEvents
) {}
