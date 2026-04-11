import cron from 'node-cron';
import { App } from '@slack/bolt';
import { CalendarService } from '../services/calendar.service';
import { WeatherService } from '../services/weather.service';
import { HealthService } from '../services/health.service';
import { FocusEngine } from './focus.logic';
import { BlockKitBuilder } from '../utils/block-kit';

export class SchedulerService {
  // 중복 알림 방지: 마지막 알림 전송 시각 기록
  private lastGoldenTimeAlert: Date | null = null;
  private lastEmergencyAlert: Date | null = null;
  private readonly ALERT_COOLDOWN_MS = 60 * 60 * 1000; // 1시간

  constructor(
    private app: App,
    private channelId: string,
    private calendarService: CalendarService,
    private weatherService: WeatherService,
    private healthService: HealthService,
    private focusEngine: FocusEngine
  ) {}

  start() {
    // 매일 오전 8시 모닝 브리핑
    cron.schedule('0 8 * * *', async () => {
      console.log('⏰ [Scheduler] Running Morning Briefing...');
      await this.sendMorningBriefing();
    });

    // 매시간 집중도 & 에너지 체크
    cron.schedule('0 * * * *', async () => {
      console.log('⏰ [Scheduler] Running Hourly Status Check...');
      await this.checkEnergyAndEvents();
    });

    console.log('📅 Automation Scheduler started.');
  }

  private async sendMorningBriefing() {
    const [weather, nextEvent] = await Promise.all([
      this.weatherService.getCurrentWeather(),
      this.calendarService.getNextEvent()
    ]);

    const blocks = BlockKitBuilder.morningBriefing({
      temp: weather?.temp || 0,
      description: weather?.description || '맑음',
      isPrecipitation: weather?.isPrecipitation || false,
      nextEvent: (nextEvent as any)?.summary || '없음',
      departureTime: '09:05 AM'
    });

    await this.app.client.chat.postMessage({
      channel: this.channelId,
      text: '오늘의 모닝 브리핑입니다!',
      blocks
    });
  }

  private async checkEnergyAndEvents() {
    const [health, todaysEvents, nextEvent] = await Promise.all([
      this.healthService.getTodaysHealthMetrics(),
      this.calendarService.getTodaysEvents(),
      this.calendarService.getNextEvent()
    ]);

    const focusScore = this.focusEngine.calculateFocusScore({ health, calendarEvents: todaysEvents });
    const now = new Date();

    // 시나리오 A: Emergency Boost — 집중도 낮고 1시간 내 일정 있을 때
    if (focusScore < 30 && nextEvent) {
      const startTime = new Date((nextEvent as any).start?.dateTime || '').getTime();
      const isWithinOneHour = startTime - now.getTime() < 3600000;
      const isCooledDown = !this.lastEmergencyAlert ||
        now.getTime() - this.lastEmergencyAlert.getTime() > this.ALERT_COOLDOWN_MS;

      if (isWithinOneHour && isCooledDown) {
        this.lastEmergencyAlert = now;
        await this.app.client.chat.postMessage({
          channel: this.channelId,
          text: '🚨 비상 에너지 부스트 알림!',
          blocks: [
            {
              type: 'section',
              text: {
                type: 'mrkdwn',
                text: `*🚨 비상 에너지 부스트 알림!*\n\n집중 점수 *${focusScore}점*으로 매우 낮습니다. 곧 *${(nextEvent as any).summary}* 일정이 시작됩니다. 지금 바로 회복하세요!`
              }
            },
            {
              type: 'actions',
              elements: [
                { type: 'button', text: { type: 'plain_text', text: '☕ 카페인 충전' }, action_id: 'boost_coffee' },
                { type: 'button', text: { type: 'plain_text', text: '🤸 5분 스트레칭' }, action_id: 'boost_stretch' }
              ]
            }
          ]
        });
        console.log('🚨 [Scheduler] Emergency Boost sent. Score:', focusScore);
      }
    }

    // 시나리오 B: 골든타임 — 집중도 높을 때 Deep Work 제안
    if (focusScore >= 85) {
      const isCooledDown = !this.lastGoldenTimeAlert ||
        now.getTime() - this.lastGoldenTimeAlert.getTime() > this.ALERT_COOLDOWN_MS;

      if (isCooledDown) {
        this.lastGoldenTimeAlert = now;
        const insight = this.focusEngine.getFocusInsight(focusScore);
        const blocks = BlockKitBuilder.focusProposal({
          focusScore,
          insight,
          recommendedTask: 'AI 서비스 기획안 초안 검토'
        });

        try {
          const result = await this.app.client.chat.postMessage({
            channel: this.channelId,
            text: '🔥 집중 골든타임 알림!',
            blocks
          });
          console.log('🔥 [Scheduler] Golden Time alert sent. Score:', focusScore, '| ts:', result.ts);
        } catch (err: any) {
          console.error('❌ [Scheduler] Golden Time postMessage 실패:', err.message);
        }
      }
    }
  }
}
