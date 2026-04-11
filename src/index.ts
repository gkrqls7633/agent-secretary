import { App } from '@slack/bolt';
import dotenv from 'dotenv';
import express from 'express';
import { google } from 'googleapis';
import fs from 'fs';
import path from 'path';
import { CalendarService } from './services/calendar.service';
import { WeatherService } from './services/weather.service';
import { HealthService } from './services/health.service';
import { DatabaseManager } from './db/database';
import { FocusEngine } from './logic/focus.logic';
import { SchedulerService } from './logic/scheduler.logic';
import { BlockKitBuilder } from './utils/block-kit';

dotenv.config();

const slackBotToken = process.env.SLACK_BOT_TOKEN || '';
const slackSigningSecret = process.env.SLACK_SIGNING_SECRET || '';
const slackAppToken = process.env.SLACK_APP_TOKEN || '';
const slackChannelId = process.env.SLACK_CHANNEL_ID || '';
const weatherApiKey = process.env.OPENWEATHER_API_KEY || '';
const port = Number(process.env.PORT) || 3000;

// Google OAuth 설정
const TOKEN_PATH = path.join(__dirname, '..', 'token.json');
const REDIRECT_URI = `http://localhost:${port}/auth/google/callback`;
const SCOPES = ['https://www.googleapis.com/auth/calendar.readonly'];

const oauth2Client = new google.auth.OAuth2(
  process.env.GOOGLE_OAUTH_CLIENT_ID,
  process.env.GOOGLE_OAUTH_CLIENT_SECRET,
  REDIRECT_URI
);

if (fs.existsSync(TOKEN_PATH)) {
  const token = JSON.parse(fs.readFileSync(TOKEN_PATH, 'utf8'));
  oauth2Client.setCredentials(token);
  console.log('✅ Google Calendar 토큰 로드 완료');
} else {
  console.log('⚠️  Google Calendar 미인증. http://localhost:' + port + '/auth/google 에서 인증하세요.');
}

// 서비스 및 엔진 초기화
const calendarService = new CalendarService(oauth2Client);
const weatherService = new WeatherService(weatherApiKey);
const healthService = new HealthService();
const dbManager = new DatabaseManager();
const focusEngine = new FocusEngine();

const webServer = express();

// Google OAuth 라우트
webServer.get('/auth/google', (_req, res) => {
  const url = oauth2Client.generateAuthUrl({ access_type: 'offline', scope: SCOPES });
  res.redirect(url);
});

webServer.get('/auth/google/callback', async (req, res) => {
  const code = req.query.code as string;
  try {
    const { tokens } = await oauth2Client.getToken(code);
    oauth2Client.setCredentials(tokens);
    fs.writeFileSync(TOKEN_PATH, JSON.stringify(tokens));
    console.log('✅ Google Calendar 인증 완료, 토큰 저장됨');
    res.send('✅ Google Calendar 인증 완료! 이 창을 닫아도 됩니다.');
  } catch (err) {
    console.error('❌ Google OAuth 콜백 오류:', err);
    res.status(500).send('인증 실패. 서버 로그를 확인하세요.');
  }
});

webServer.get('/debug', async (_req, res) => {
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

// Slack 앱 설정
const app = new App({
  token: slackBotToken,
  signingSecret: slackSigningSecret,
  socketMode: true,
  appToken: slackAppToken,
});

// [Scenario 1] 모닝 브리핑
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

  await say({ text: '모닝 브리핑', blocks });
});

// [Scenario 2] 집중 업무 제안
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

  await say({ text: '집중 업무 제안', blocks });
});

// [Scenario 3] 회복 제안
app.message('restore', async ({ say }) => {
  const health = await healthService.getTodaysHealthMetrics();
  const blocks = BlockKitBuilder.restorationProposal({
    recoveryLevel: health.recoveryLevel,
    fatigueLevel: 100 - health.recoveryLevel
  });

  await say({ text: '회복 제안', blocks });
});

// [Action] 모닝 브리핑 확인
app.action('morning_confirm', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'morning', proposal_text: '모닝 브리핑 확인', user_action: 'accepted' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '출근 준비 완료!',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '✅ *출근 준비 완료!* 안전하게 다녀오세요.' } }]
  });
});

// [Action] 집중 업무 시작
app.action('focus_start', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'focus', proposal_text: '집중 업무 제안', user_action: 'accepted' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '집중 모드 가동!',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '🔥 *집중 모드 가동!*' } }]
  });
});

// [Action] 집중 업무 30분 뒤
app.action('focus_delay', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'focus', proposal_text: '집중 업무 제안', user_action: 'snoozed' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '30분 뒤 다시 알림',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '⏰ *알겠습니다.* 30분 뒤에 다시 알려드릴게요.' } }]
  });
});

// [Action] 집중 업무 거절
app.action('focus_reject', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'focus', proposal_text: '집중 업무 제안', user_action: 'rejected', rejection_reason: '취향 아님/피로' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '제안 취소',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '🏃‍♂️ *이해했습니다.* 나중에 다시 제안해 드릴게요.' } }]
  });
});

// [Action] 회복 제안 수락
app.action('restore_accept', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'restoration', proposal_text: '회복 제안', user_action: 'accepted' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '회복 시간 시작',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '🧘 *회복 시간을 시작합니다.* 잠시 쉬어가세요.' } }]
  });
});

// [Action] 회복 제안 거절
app.action('restore_reject', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'restoration', proposal_text: '회복 제안', user_action: 'rejected' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '회복 제안 거절',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '💪 *알겠습니다.* 계속 진행하세요!' } }]
  });
});

// [Action] 비상 에너지 부스트 - 카페인
app.action('boost_coffee', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'emergency_boost', proposal_text: '비상 에너지 부스트', user_action: 'accepted', rejection_reason: 'boost_coffee' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '카페인 충전!',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '☕ *카페인 충전!* 10분 안에 돌아오세요. 집중력이 올라갈 거예요.' } }]
  });
});

// [Action] 비상 에너지 부스트 - 스트레칭
app.action('boost_stretch', async ({ ack, body, client }) => {
  await ack();
  await dbManager.logInteraction({ scenario: 'emergency_boost', proposal_text: '비상 에너지 부스트', user_action: 'accepted', rejection_reason: 'boost_stretch' });
  await client.chat.update({
    channel: (body as any).container.channel_id,
    ts: (body as any).container.message_ts,
    text: '5분 스트레칭!',
    blocks: [{ type: 'section', text: { type: 'mrkdwn', text: '🤸 *5분 스트레칭 시작!* 혈액순환이 되면 집중력이 돌아옵니다.' } }]
  });
});

(async () => {
  try {
    await dbManager.init();
    await app.start();

    // 스케줄러 시작
    const scheduler = new SchedulerService(
      app, slackChannelId,
      calendarService, weatherService, healthService, focusEngine
    );
    scheduler.start();

    console.log('⚡️ Secretary Agent is online with Interaction Loop!');
  } catch (error) {
    console.error('❌ 앱 구동 실패:', error);
  }
})();
