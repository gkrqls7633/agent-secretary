export interface HealthData {
  sleepScore: number; // 0-100
  sleepHours: number;
  heartRateVariability: number; // 집중도와 상관관계가 높은 지표
  recoveryLevel: number; // 0-100
}

export class HealthService {
  /**
   * 사용자(학빈)의 오늘의 건강 상태 시뮬레이션 데이터를 반환합니다.
   * 실제 연동 전까지는 이 모킹 데이터를 사용합니다.
   */
  async getTodaysHealthMetrics(): Promise<HealthData> {
    // 실제로는 Apple Health API 등에서 동기화된 DB 값을 읽어오는 로직이 들어갈 자리입니다.
    return {
      sleepScore: 85,
      sleepHours: 7.5,
      heartRateVariability: 45,
      recoveryLevel: 90
    };
  }

  /**
   * 실시간 피로도를 추정합니다.
   */
  async getCurrentFatigueLevel(): Promise<number> {
    const health = await this.getTodaysHealthMetrics();
    // 복구 수준이 높을수록 피로도는 낮다고 가정 (0-100)
    return 100 - health.recoveryLevel;
  }
}
