package com.agent.secretary.interfaces.slack;

import com.agent.secretary.application.service.AgentSecretaryService;
import com.agent.secretary.domain.model.Task;
import com.slack.api.bolt.App;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final AgentSecretaryService agentSecretaryService;
    private final SlackBlockKitGenerator blockKitGenerator = new SlackBlockKitGenerator();
    private final App slackApp;

    @Value("${slack.channel-id}")
    private String channelId;

    @PostConstruct
    public void registerSlackHandlers() {
        log.info("Initializing Slack handlers...");

        // 일반 메시지 이벤트 무시 처리
        slackApp.event(MessageEvent.class, (req, ctx) -> ctx.ack());

        // 1. 앱 멘션 이벤트 처리 - "task" 키워드 포함 시 Task 목록, 아니면 집중도 체크
        slackApp.event(AppMentionEvent.class, (req, ctx) -> {
            String text = req.getEvent().getText().toLowerCase();
            log.info("Event Received: App Mentioned -> {}", text);
            if (text.contains("task") || text.contains("할 일") || text.contains("태스크")) {
                return handleTaskList(ctx);
            }
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

        // 5. Task 추가 버튼 처리 — 모달 오픈
        slackApp.blockAction("task_add", (req, ctx) -> {
            ctx.client().viewsOpen(r -> r
                .triggerId(req.getPayload().getTriggerId())
                .view(blockKitGenerator.buildTaskAddModal())
            );
            return ctx.ack();
        });

        // 6. Task 수정 버튼 처리 — 기존 데이터 pre-fill 모달 오픈
        slackApp.blockAction(Pattern.compile("task_edit_(.+)"), (req, ctx) -> {
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String taskId = actionId.replace("task_edit_", "");
            String triggerId = req.getPayload().getTriggerId();
            agentSecretaryService.getTaskById(taskId).thenAccept(task -> {
                try {
                    ctx.client().viewsOpen(r -> r
                        .triggerId(triggerId)
                        .view(blockKitGenerator.buildTaskEditModal(task))
                    );
                } catch (Exception e) {
                    log.error("Failed to open edit modal. taskId={}", taskId, e);
                }
            });
            return ctx.ack();
        });

        // 8. 태스크 추가 모달 제출 처리
        slackApp.viewSubmission("task_add_modal", (req, ctx) -> {
            var values = req.getPayload().getView().getState().getValues();
            String title = values.get("title_block").get("title_input").getValue();
            String notes = values.get("notes_block").get("notes_input").getValue();
            Task newTask = Task.createNew(title, notes, null);
            agentSecretaryService.saveTask(newTask).thenRun(() -> {
                try {
                    ctx.client().chatPostMessage(r -> r
                        .channel(channelId)
                        .text("✅ 태스크 *" + title + "* 가 추가되었습니다.")
                    );
                } catch (Exception e) {
                    log.error("Failed to notify after task add", e);
                }
            });
            return ctx.ack();
        });

        // 9. 태스크 수정 모달 제출 처리
        slackApp.viewSubmission("task_edit_modal", (req, ctx) -> {
            String taskId = req.getPayload().getView().getPrivateMetadata();
            var values = req.getPayload().getView().getState().getValues();
            String title = values.get("title_block").get("title_input").getValue();
            String notes = values.get("notes_block").get("notes_input").getValue();
            
            agentSecretaryService.getTaskById(taskId).thenAccept(existingTask -> {
                Task updatedTask = new Task(
                    taskId, 
                    title, 
                    notes, 
                    existingTask.completed(), 
                    existingTask.dueAt(), 
                    existingTask.completedAt(), 
                    existingTask.hasTime()
                );
                agentSecretaryService.updateTask(updatedTask).thenRun(() -> {
                    try {
                        ctx.client().chatPostMessage(r -> r
                            .channel(channelId)
                            .text("✏️ 태스크 *" + title + "* 가 수정되었습니다.")
                        );
                    } catch (Exception e) {
                        log.error("Failed to notify after task edit", e);
                    }
                });
            });
            return ctx.ack();
        });

        // checkin. 저녁 체크인 완료 버튼 처리 (action_id: checkin_complete_{taskId})
        slackApp.blockAction(Pattern.compile("checkin_complete_(.+)"), (req, ctx) -> {
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String taskId = actionId.replace("checkin_complete_", "");
            log.info("Action Received: checkin_complete -> taskId={}", taskId);

            agentSecretaryService.markEveningTaskComplete(taskId).thenAccept(updatedBlocks -> {
                String ts = agentSecretaryService.getEveningCheckInTs();
                if (ts == null) return;
                try {
                    ctx.client().chatUpdate(r -> r
                        .channel(channelId)
                        .ts(ts)
                        .blocks(updatedBlocks)
                        .text("오늘 하루 마무리 체크인입니다.")
                    );
                } catch (Exception e) {
                    log.error("Failed to update evening check-in message", e);
                }
            });
            return ctx.ack();
        });

        // 7. Task 완료 버튼 처리 (action_id: task_complete_{taskId})
        slackApp.blockAction(Pattern.compile("task_complete_(.+)"), (req, ctx) -> {
            String actionId = req.getPayload().getActions().get(0).getActionId();
            String taskId = actionId.replace("task_complete_", "");
            log.info("Action Received: task_complete -> taskId={}", taskId);

            agentSecretaryService.updateTaskStatus(taskId, true).thenRun(() -> {
                try {
                    ctx.respond(r -> r.text("✅ 태스크를 완료 처리했습니다!").replaceOriginal(false));
                } catch (Exception e) {
                    log.error("Failed to respond after task update", e);
                }
            });
            return ctx.ack();
        });

        log.info("All Slack handlers (Events & Block Actions) have been registered.");
    }

    private Response handleTaskList(com.slack.api.bolt.context.builtin.EventContext ctx) {
        agentSecretaryService.getTodaysTasks().thenAccept(tasks -> {
            var blocks = blockKitGenerator.generateDailyBriefing(tasks, java.time.LocalDate.now());
            try {
                ctx.client().chatPostMessage(r -> r
                        .channel(ctx.getChannelId())
                        .blocks(blocks)
                        .text("오늘의 할 일 목록입니다.")
                );
            } catch (Exception e) {
                log.error("Failed to post task list to slack", e);
            }
        });
        return ctx.ack();
    }

    private Response handleStatusCheck(com.slack.api.bolt.context.builtin.EventContext ctx) {
        agentSecretaryService.analyzeCurrentStatus().thenAccept(result -> {
            var blocks = blockKitGenerator.generateFocusProposal(
                    result.score(),
                    result.insight(),
                    "구글 캘린더 기반 추천 업무",
                    result.events()
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
