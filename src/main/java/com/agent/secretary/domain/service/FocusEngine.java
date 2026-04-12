package com.agent.secretary.domain.service;

import com.agent.secretary.domain.model.FocusContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 학빈님의 집중도 점수를 산출하는 도메인 서비스.
 * 프레임워크 의존성 없이 순수 비즈니스 로직만 포함.
 */
@Slf4j
public class FocusEngine {

    /**
     * 집중도 점수(0~100)를 산출합니다.
     * 가중치: 수면 점수(40%) + 일정 밀집도(30%) + 회복 수준(30%)
     */
    public int calculateFocusScore(FocusContext context) {
        var health = context.health();
        var calendarEvents = context.calendarEvents();

        // 1. 수면 점수 가중치 (40%)
        double sleepWeight = 0.4;
        double sleepScoreValue = (health.sleepScore() / 100.0) * 100 * sleepWeight;

        // 2. 일정 밀집도 가중치 (30%)
        double scheduleWeight = 0.3;
        int todaysMeetingCount = calendarEvents.size();
        
        // 하루 일정이 0-2개면 100점, 5개 이상이면 0점 (역상관)
        int scheduleScore = 100 - (Math.min(todaysMeetingCount, 5) * 20);
        double scheduleScoreValue = (scheduleScore / 100.0) * 100 * scheduleWeight;

        // 3. 실시간 회복 및 HRV 가중치 (30%)
        double recoveryWeight = 0.3;
        double recoveryScoreValue = (health.recoveryLevel() / 100.0) * 100 * recoveryWeight;

        // 총합 계산 (0-100)
        int totalScore = (int) Math.round(sleepScoreValue + scheduleScoreValue + recoveryScoreValue);

        return Math.clamp(totalScore, 0, 100);
    }

    /**
     * 집중도 점수에 따른 인사이트 메시지를 생성합니다.
     */
    public String getFocusInsight(int score) {
        if (score >= 85) return "🔥 최상의 컨디션입니다! 지금 바로 Deep Work를 시작하세요.";
        if (score >= 65) return "✅ 업무 집중도가 좋습니다. 창의적인 기획 업무에 적합합니다.";
        if (score >= 40) return "⚙️ 보통의 상태입니다. 루틴 업무(Shallow Work)를 처리하기 좋습니다.";
        if (score >= 20) return "🔋 에너지가 소진되고 있습니다. 짧은 휴식이나 가벼운 스트레칭을 추천합니다.";
        return "🛌 인지 부하가 한계에 도달했습니다. 무리한 업무보다는 회복에 집중하세요.";
    }
}
