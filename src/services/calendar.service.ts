import { google, calendar_v3, Auth } from 'googleapis';

export class CalendarService {
  private calendar: calendar_v3.Calendar;

  constructor(auth: Auth.OAuth2Client) {
    this.calendar = google.calendar({
      version: 'v3',
      auth
    });
  }

  /**
   * 오늘의 일정 리스트를 가져옵니다. (Read-only)
   * @param calendarId 캘린더 ID (기본값: 'primary')
   */
  async getTodaysEvents(calendarId: string = 'primary') {
    const now = new Date();
    const startOfDay = new Date(now.setHours(0, 0, 0, 0)).toISOString();
    const endOfDay = new Date(now.setHours(23, 59, 59, 999)).toISOString();

    try {
      const response = await this.calendar.events.list({
        calendarId,
        timeMin: startOfDay,
        timeMax: endOfDay,
        singleEvents: true,
        orderBy: 'startTime',
      });

      return response.data.items || [];
    } catch (error) {
      console.error('❌ Google Calendar API 호출 오류:', error);
      return [];
    }
  }

  /**
   * 다음 가장 가까운 일정을 가져옵니다.
   */
  async getNextEvent(calendarId: string = 'primary') {
    const events = await this.getTodaysEvents(calendarId);
    const now = new Date().getTime();
    
    return events.find(event => {
      const startTime = new Date(event.start?.dateTime || event.start?.date || '').getTime();
      return startTime > now;
    });
  }
}
