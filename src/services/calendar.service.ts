import { google, calendar_v3 } from 'googleapis';

export class CalendarService {
  private calendar: calendar_v3.Calendar;

  constructor(apiKey: string) {
    // API 키를 사용한 인증 설정 (타입 에러 방지를 위해 any 캐스팅 또는 객체 형태 사용)
    this.calendar = google.calendar({
      version: 'v3',
      auth: apiKey // googleapis는 auth에 string(API Key)도 허용하지만 타입 정의가 엄격할 수 있음
    });
  }

  /**
   * 오늘의 일정 리스트를 가져옵니다. (Read-only)
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
    } catch (error: any) {
      // 401 에러 등이 발생할 경우 상세 로깅
      console.error('❌ Google Calendar API 호출 오류:', error.response?.data || error.message);
      return [];
    }
  }

  /**
   * 다음 가장 가까운 일정을 가져옵니다.
   */
  async getNextEvent(calendarId: string = 'primary') {
    try {
      const events = await this.getTodaysEvents(calendarId);
      const now = new Date().getTime();
      
      return events.find(event => {
        const startTime = new Date(event.start?.dateTime || event.start?.date || '').getTime();
        return startTime > now;
      });
    } catch (error) {
      return null;
    }
  }
}
