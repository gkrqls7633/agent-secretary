import { HealthData } from '../services/health.service';

export interface FocusContext {
  health: HealthData;
  calendarEvents: any[];
}

export class FocusEngine {
  /**
   * 학빈님의 현재 집중도 점수(0~100)를 산출합니다.
   * 가중치: 수면 점수(40%) + 일정 밀집도(30%) + 회복 수준(30%)
   */
  calculateFocusScore(context: FocusContext): number {
    const { health, calendarEvents } = context;

    // 1. 수면 점수 가중치 (40%)
    const sleepWeight = 0.4;
    const sleepScoreValue = (health.sleepScore / 100) * 100 * sleepWeight;

    // 2. 일정 밀집도 가중치 (30%)
    // 일정이 많을수록 인지적 부하(Cognitive Load)가 커져 집중도가 낮아진다고 가정
    const scheduleWeight = 0.3;
    const todaysMeetingCount = calendarEvents.length;
    
    // 하루 일정이 0-2개면 100점, 5개 이상이면 0점 (역상관)
    let scheduleScore = 100 - (Math.min(todaysMeetingCount, 5) * 20);
    const scheduleScoreValue = (scheduleScore / 100) * 100 * scheduleWeight;

    // 3. 실시간 회복 및 HRV 가중치 (30%)
    const recoveryWeight = 0.3;
    const recoveryScoreValue = (health.recoveryLevel / 100) * 100 * recoveryWeight;

    // 총합 계산 (0-100)
    const totalScore = Math.round(sleepScoreValue + scheduleScoreValue + recoveryScoreValue);

    return Math.min(Math.max(totalScore, 0), 100);
  }

  /**
   * 집중도 점수에 따른 인사이트 메시지를 생성합니다.
   */
  getFocusInsight(score: number): string {
    if (score >= 85) return '🔥 최상의 컨디션입니다! 지금 바로 Deep Work를 시작하세요.';
    if (score >= 65) return '✅ 업무 집중도가 좋습니다. 창의적인 기획 업무에 적합합니다.';
    if (score >= 40) return '⚙️ 보통의 상태입니다. 루틴 업무(Shallow Work)를 처리하기 좋습니다.';
    if (score >= 20) return '🔋 에너지가 소진되고 있습니다. 짧은 휴식이나 가벼운 스트레칭을 추천합니다.';
    return '🛌 인지 부하가 한계에 도달했습니다. 무리한 업무보다는 회복에 집중하세요.';
  }
}
