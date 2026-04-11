import { App } from '@slack/bolt';
import dotenv from 'dotenv';
import express from 'express';
import { CalendarService } from './services/calendar.service';
import { WeatherService } from './services/weather.service';
import { HealthService } from './services/health.service';
import { DatabaseManager } from './db/database';
import { FocusEngine } from './logic/focus.logic';
import { BlockKitBuilder } from './utils/block-kit';

dotenv.config();

const slackBotToken = process.env.SLACK_BOT_TOKEN || '';
const slackSigningSecret = process.env.SLACK_SIGNING_SECRET || '';
const slackAppToken = process.env.SLACK_APP_TOKEN || '';
const googleApiKey = process.env.GOOGLE_CALENDAR_API_KEY || '';
const weatherApiKey = process.env.OPENWEATHER_API_KEY || '';
const port = Number(process.env.PORT) || 3000;

// 서비스 및 엔진 초기화
const calendarService = new CalendarService(googleApiKey);
const weatherService = new WeatherService(weatherApiKey);
const healthService = new HealthService();
const dbManager = new DatabaseManager();
const focusEngine = new FocusEngine();

const webServer = express();

/**
 * 1. 데이터 통합 및 디버그 페이지
 */
webServer.get('/debug', async (req, res) => {
  try {
    const [weather, health, todaysEvents] = await Promise.all([
      weatherService.getCurrentWeather(),
      healthService.getTodaysHealthMetrics(),
      calendarService.getTodaysEvents()
    ]);

    const focusScore = focusEngine.calculateFocusScore({ health, calendarEvents: todaysEvents });
    const focusInsight = focusEngine.getFocusInsight(focusScore);

    res.json({
      status: 'success',
      timestamp: new Date().toISOString(),
      analysis: { focus_score: focusScore, insight: focusInsight },
      data: { weather, health, todays_meeting_count: todaysEvents.length }
    });
  } catch (err: any) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

webServer.listen(port, () => console.log(`🌐 Dashboard: http://localhost:${port}/debug`));

/**
 * 2. Slack 앱 설정
 */
const app = new App({
  token: slackBotToken,
  signingSecret: slackSigningSecret,
  socketMode: true,
  appToken: slackAppToken,
});

/**
 * [Scenario 1] 모닝 브리핑 수동 요청
 */
app.message('morning', async ({ say }) => {
  const [weather, nextEvent] = await Promise.all([
    weatherService.getCurrentWeather(),
    calendarService.getNextEvent()
  ]);

  const blocks = BlockKitBuilder.morningBriefing({
    temp: weather?.temp || 0,
    description: weather?.description || '맑음',
    isPrecipitation: weather?.isPrecipitation || false,
    nextEvent: (nextEvent as any)?.summary || '없음',
    departureTime: '09:05 AM'
  });

  await say({ blocks });
});

/**
 * [Scenario 2] 집중 업무 제안 수동 요청
 */
app.message('focus', async ({ say }) => {
  const [health, todaysEvents] = await Promise.all([
    healthService.getTodaysHealthMetrics(),
    calendarService.getTodaysEvents()
  ]);

  const focusScore = focusEngine.calculateFocusScore({ health, calendarEvents: todaysEvents });
  const focusInsight = focusEngine.getFocusInsight(focusScore);

  const blocks = BlockKitBuilder.focusProposal({
    focusScore,
    insight: focusInsight,
    recommendedTask: 'AI 서비스 기획안 초안 검토'
  });

  await say({ blocks });
});

/**
 * [Interaction] 상호작용 피드백 핸들러
 */

// 1. 모닝 브리핑 확인
app.action('morning_confirm', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({
    scenario: 'morning',
    proposal_text: '모닝 브리핑 확인',
    user_action: 'accepted'
  });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: "확인되었습니다! 좋은 하루 되세요. 🚗",
    blocks: [{ type: "section", text: { type: "mrkdwn", text: "✅ *출근 준비 완료!* 안전하게 다녀오세요." } }]
  });
});

// 2. 집중 업무 시작
app.action('focus_start', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({
    scenario: 'focus',
    proposal_text: '집중 업무 제안',
    user_action: 'accepted'
  });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: "집중 모드 가동! 🤫",
    blocks: [{ type: "section", text: { type: "mrkdwn", text: "🔥 *집중 모드 가동!* Slack 상태를 '방해 금지'로 변경했습니다. (시뮬레이션)" } }]
  });
});

// 3. 집중 업무 거절
app.action('focus_reject', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({
    scenario: 'focus',
    proposal_text: '집중 업무 제안',
    user_action: 'rejected',
    rejection_reason: '취향 아님/피로'
  });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: "제안을 취소했습니다.",
    blocks: [{ type: "section", text: { type: "mrkdwn", text: "🏃‍♂️ *이해했습니다.* 지금은 쉬고 싶으시군요. 나중에 다시 제안해 드릴게요." } }]
  });
});

(async () => {
  try {
    await dbManager.init();
    await app.start();
    console.log('⚡️ Secretary Agent is online with Interaction Loop!');
  } catch (error) {
    console.error('❌ 앱 구동 실패:', error);
  }
})();
