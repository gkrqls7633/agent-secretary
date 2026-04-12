package com.agent.secretary.domain.model;

/**
 * 사용자(학빈)의 건강 상태 도메인 엔터티 (Record)
 */
public record HealthData(
    int sleepScore,
    double sleepHours,
    int heartRateVariability,
    int recoveryLevel
) {}
