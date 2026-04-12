package com.agent.secretary.interfaces.slack;

import com.agent.secretary.application.service.AgentSecretaryService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Slack Bolt API 핸들러 (Slack Controller).
 * Inbound Adapter로서 사용자의 요청을 처리하고 애플리케이션 서비스를 호출.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final AgentSecretaryService agentSecretaryService;
    private final SlackBlockKitGenerator blockKitGenerator = new SlackBlockKitGenerator();
    private final App slackApp;

    @PostConstruct
    public void registerSlackHandlers() {
        // 1. 앱 멘션 이벤트 처리
        slackApp.event(AppMentionEvent.class, (req, ctx) -> {
            log.info("App Mentioned: {}", req.getEvent().getText());
            return handleStatusCheck(ctx);
        });

        // 2. 버튼 클릭 액션 처리 (집중 시작)
        slackApp.blockAction("focus_start", (req, ctx) -> {
            ctx.respond("🚀 업무 시작! 응원하겠습니다.");
            return ctx.ack();
        });

        // 3. 버튼 클릭 액션 처리 (30분 지연)
        slackApp.blockAction("focus_delay", (req, ctx) -> {
            ctx.respond("💤 알겠습니다. 30분 뒤에 다시 제안 드릴게요.");
            return ctx.ack();
        });

        // 4. 버튼 클릭 액션 처리 (거절)
        slackApp.blockAction("focus_reject", (req, ctx) -> {
            ctx.respond("✅ 네, 무리하지 마세요! 필요할 때 다시 불러주세요.");
            return ctx.ack();
        });
    }

    private Response handleStatusCheck(com.slack.api.bolt.context.builtin.EventContext ctx) {
        agentSecretaryService.analyzeCurrentStatus().thenAccept(result -> {
            var blocks = blockKitGenerator.generateFocusProposal(
                    result.score(),
                    result.insight(),
                    "AI 서비스 기획안 초안 검토"
            );

            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(ctx.getChannelId())
                        .blocks(blocks)
                );
            } catch (Exception e) {
                log.error("Failed to post message to slack", e);
            }
        });

        return ctx.ack();
    }
}
