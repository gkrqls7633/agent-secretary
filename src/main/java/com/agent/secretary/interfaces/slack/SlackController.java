package com.agent.secretary.interfaces.slack;

import com.agent.secretary.application.service.AgentSecretaryService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final AgentSecretaryService agentSecretaryService;
    private final SlackBlockKitGenerator blockKitGenerator = new SlackBlockKitGenerator();
    private final App slackApp;

    @PostConstruct
    public void registerSlackHandlers() {
        log.info("Initializing Slack handlers...");

        // 1. 앱 멘션 이벤트 처리
        slackApp.event(AppMentionEvent.class, (req, ctx) -> {
            log.info("Event Received: App Mentioned -> {}", req.getEvent().getText());
            return handleStatusCheck(ctx);
        });

        // 2. 버튼 클릭 액션 처리 (집중 시작)
        slackApp.blockAction("focus_start", (req, ctx) -> {
            log.info("Action Received: focus_start");
            ctx.respond(r -> r.text("🚀 업무 시작! 응원하겠습니다.").replaceOriginal(true));
            return ctx.ack();
        });

        // 3. 버튼 클릭 액션 처리 (30분 지연)
        slackApp.blockAction("focus_delay", (req, ctx) -> {
            log.info("Action Received: focus_delay");
            ctx.respond(r -> r.text("💤 알겠습니다. 30분 뒤에 다시 제안 드릴게요.").replaceOriginal(true));
            return ctx.ack();
        });

        // 4. 버튼 클릭 액션 처리 (거절)
        slackApp.blockAction("focus_reject", (req, ctx) -> {
            log.info("Action Received: focus_reject");
            ctx.respond(r -> r.text("✅ 네, 무리하지 마세요! 필요할 때 다시 불러주세요.").replaceOriginal(true));
            return ctx.ack();
        });

        log.info("All Slack handlers (Events & Block Actions) have been registered.");
    }

    private Response handleStatusCheck(com.slack.api.bolt.context.builtin.EventContext ctx) {
        agentSecretaryService.analyzeCurrentStatus().thenAccept(result -> {
            var blocks = blockKitGenerator.generateFocusProposal(
                    result.score(),
                    result.insight(),
                    "구글 캘린더 기반 추천 업무"
            );

            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(ctx.getChannelId())
                        .blocks(blocks)
                        .text("집중 골든타임 알림입니다.") // 텍스트 필드 추가 (Best Practice)
                );
            } catch (Exception e) {
                log.error("Failed to post message to slack", e);
            }
        });

        return ctx.ack();
    }
}
