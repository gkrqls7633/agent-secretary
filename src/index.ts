import { App } from '@slack/bolt';
import dotenv from 'dotenv';
import express from 'express';
import { google } from 'googleapis';
import fs from 'fs';
import path from 'path';
import { CalendarService } from './services/calendar.service';
import { WeatherService } from './services/weather.service';
import { HealthService } from './services/health.service';

dotenv.config();

const slackBotToken = process.env.SLACK_BOT_TOKEN || '';
const slackSigningSecret = process.env.SLACK_SIGNING_SECRET || '';
const slackAppToken = process.env.SLACK_APP_TOKEN || '';
const weatherApiKey = process.env.OPENWEATHER_API_KEY || '';
const port = Number(process.env.PORT) || 3000;

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
  console.log('⚠️  Google Calendar 미인증 상태. http://localhost:' + port + '/auth/google 에서 인증하세요.');
}

const calendarService = new CalendarService(oauth2Client);
const weatherService = new WeatherService(weatherApiKey);
const healthService = new HealthService();

const webServer = express();

webServer.get('/auth/google', (req, res) => {
  const url = oauth2Client.generateAuthUrl({
    access_type: 'offline',
    scope: SCOPES,
  });
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

webServer.get('/debug', async (req, res) => {
  console.log('--- Debug Request Received ---');
  try {
    const weather = await weatherService.getCurrentWeather();
    const health = await healthService.getTodaysHealthMetrics();
    const nextEvent = await calendarService.getNextEvent().catch(err => {
      console.error('❌ Calendar Error:', err.message);
      return { summary: 'Calendar Error: ' + err.message };
    });

    res.json({
      status: 'success',
      timestamp: new Date().toISOString(),
      weather: weather || 'Failed (Check server logs)',
      health: health,
      calendar: nextEvent || 'No upcoming events'
    });
  } catch (err: any) {
    console.error('❌ Debug Route Error:', err);
    res.status(500).json({ status: 'error', message: err.message });
  }
});

webServer.listen(port, () => console.log(`🌐 Dashboard: http://localhost:${port}/debug`));

const app = new App({
  token: slackBotToken,
  signingSecret: slackSigningSecret,
  socketMode: true,
  appToken: slackAppToken,
});

// 모든 메시지 로깅 (슬랙 이벤트 수신 확인용)
app.event('message', async ({ event }) => {
  console.log('📥 Incoming Slack Message:', (event as any).text);
});

app.message('check', async ({ message, say }) => {
  console.log('✅ "check" command triggered');
  try {
    const [weather, health, nextEvent] = await Promise.all([
      weatherService.getCurrentWeather(),
      healthService.getTodaysHealthMetrics(),
      calendarService.getNextEvent()
    ]);

    await say({
      blocks: [
        {
          type: "header",
          text: { type: "plain_text", text: "📊 현재 데이터 수집 리포트", emoji: true }
        },
        {
          type: "section",
          fields: [
            { type: "mrkdwn", text: `*날씨:*\n${weather ? `${weather.temp}°C, ${weather.description}` : '날씨 정보를 가져올 수 없습니다.'}` },
            { type: "mrkdwn", text: `*에너지 점수:*\n${health.sleepScore}점 (회복 ${health.recoveryLevel}%)` }
          ]
        },
        {
          type: "section",
          text: {
            type: "mrkdwn",
            text: `*다음 일정:*\n${nextEvent ? (nextEvent as any).summary : '일정 없음'}`
          }
        }
      ]
    });
  } catch (error) {
    console.error('❌ Check Error:', error);
    await say('데이터 조회 중 오류가 발생했습니다.');
  }
});

(async () => {
  try {
    await app.start();
    console.log('⚡️ Secretary Agent is online!');
  } catch (error) {
    console.error('❌ Slack Bot start failed:', error);
  }
})();
