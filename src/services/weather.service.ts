import axios from 'axios';

export interface WeatherData {
  temp: number;
  condition: string;
  isPrecipitation: boolean;
  description: string;
}

export class WeatherService {
  private apiKey: string;
  private baseUrl = 'https://api.openweathermap.org/data/2.5/weather';

  constructor(apiKey: string) {
    this.apiKey = apiKey;
  }

  async getCurrentWeather(city: string = 'Seoul'): Promise<WeatherData | null> {
    try {
      if (!this.apiKey) throw new Error('Weather API Key is missing');

      const response = await axios.get(this.baseUrl, {
        params: {
          q: city,
          appid: this.apiKey,
          units: 'metric',
          lang: 'kr'
        }
      });

      const data = response.data;
      const weatherId = data.weather[0].id;

      return {
        temp: data.main.temp,
        condition: data.weather[0].main,
        isPrecipitation: weatherId >= 200 && weatherId < 700,
        description: data.weather[0].description
      };
    } catch (error: any) {
      // 상세 에러 출력
      console.error('❌ Weather API Error:', error.response?.data || error.message);
      return null;
    }
  }
}
