export class BlockKitBuilder {
  /**
   * 모닝 브리핑 메시지 템플릿
   */
  static morningBriefing(data: {
    temp: number;
    description: string;
    isPrecipitation: boolean;
    nextEvent: string;
    departureTime: string;
  }) {
    return [
      {
        type: "header",
        text: { type: "plain_text", text: "☀️ 굿모닝, 학빈님!", emoji: true }
      },
      {
        type: "section",
        text: {
          type: "mrkdwn",
          text: `오늘의 첫 일정은 *${data.nextEvent}*입니다. \n현재 서울의 날씨는 ${data.temp}°C, ${data.description}입니다.`
        }
      },
      {
        type: "section",
        fields: [
          { type: "mrkdwn", text: `*출발 추천 시각:*\n${data.departureTime}` },
          { type: "mrkdwn", text: `*체크리스트:*\n${data.isPrecipitation ? '☂️ 우산 챙기기!' : '✅ 가벼운 옷차림'}` }
        ]
      },
      {
        type: "actions",
        elements: [
          {
            type: "button",
            text: { type: "plain_text", text: "확인했습니다!", emoji: true },
            style: "primary",
            action_id: "morning_confirm"
          }
        ]
      }
    ];
  }

  /**
   * 회복 제안 메시지 템플릿
   */
  static restorationProposal(data: {
    recoveryLevel: number;
    fatigueLevel: number;
  }) {
    const suggestion = data.fatigueLevel >= 70
      ? '😴 짧은 낮잠 (10-20분)'
      : data.fatigueLevel >= 40
      ? '🧘 가벼운 스트레칭 또는 산책'
      : '☕ 커피 한 잔과 짧은 휴식';

    return [
      {
        type: 'header',
        text: { type: 'plain_text', text: '🔋 회복 타임 제안', emoji: true }
      },
      {
        type: 'section',
        text: {
          type: 'mrkdwn',
          text: `현재 회복 수준 *${data.recoveryLevel}%* / 피로도 *${data.fatigueLevel}%*\n지금은 잠깐 쉬어가는 게 좋을 것 같아요.`
        }
      },
      {
        type: 'section',
        text: { type: 'mrkdwn', text: `추천: *${suggestion}*` }
      },
      {
        type: 'actions',
        elements: [
          {
            type: 'button',
            text: { type: 'plain_text', text: '쉴게요', emoji: true },
            style: 'primary',
            action_id: 'restore_accept'
          },
          {
            type: 'button',
            text: { type: 'plain_text', text: '괜찮아요', emoji: true },
            action_id: 'restore_reject'
          }
        ]
      }
    ];
  }

  /**
   * 집중 업무 제안 메시지 템플릿
   */
  static focusProposal(data: {
    focusScore: number;
    insight: string;
    recommendedTask: string;
  }) {
    return [
      {
        type: "header",
        text: { type: "plain_text", text: "🚀 집중 골든타임 알림", emoji: true }
      },
      {
        type: "section",
        text: {
          type: "mrkdwn",
          text: `*현재 집중 점수: ${data.focusScore}점*\n${data.insight}`
        }
      },
      {
        type: "section",
        text: {
          type: "mrkdwn",
          text: `지금 바로 *[${data.recommendedTask}]* 업무를 시작해 보시는 건 어떨까요?`
        }
      },
      {
        type: "actions",
        elements: [
          {
            type: "button",
            text: { type: "plain_text", text: "지금 시작", emoji: true },
            style: "primary",
            action_id: "focus_start"
          },
          {
            type: "button",
            text: { type: "plain_text", text: "30분 뒤에", emoji: true },
            action_id: "focus_delay"
          },
          {
            type: "button",
            text: { type: "plain_text", text: "안 할래", emoji: true },
            style: "danger",
            action_id: "focus_reject"
          }
        ]
      }
    ];
  }
}
