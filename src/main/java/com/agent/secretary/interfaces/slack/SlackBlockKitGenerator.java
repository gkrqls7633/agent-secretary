package com.agent.secretary.interfaces.slack;

import com.agent.secretary.domain.model.CalendarEvent;
import com.agent.secretary.domain.model.Task;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElements;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

/**
 * Slack Block Kit을 생성하는 전용 Generator 클래스.
 */
public class SlackBlockKitGenerator {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 모닝 브리핑 메시지 템플릿
     */
    public List<LayoutBlock> generateMorningBriefing(
            double temp,
            String description,
            boolean isPrecipitation,
            String nextEvent,
            String departureTime
    ) {
        return asBlocks(
            header(h -> h.text(plainText("☀️ 굿모닝, 학빈님!", true))),
            section(s -> s.text(markdownText(String.format(
                    "오늘의 첫 일정은 *%s*입니다. \n현재 서울의 날씨는 %.1f°C, %s입니다.",
                    nextEvent, temp, description)))),
            section(s -> s.fields(asSectionFields(
                    markdownText("*출발 추천 시각:*\n" + departureTime),
                    markdownText("*체크리스트:*\n" + (isPrecipitation ? "☂️ 우산 챙기기!" : "✅ 가벼운 옷차림"))
            ))),
            actions(a -> a.elements(asElements(
                    button(b -> b.text(plainText("확인했습니다!", true))
                            .style("primary")
                            .actionId("morning_confirm"))
            )))
        );
    }

    /**
     * 집중 업무 제안 메시지 템플릿
     */
    public List<LayoutBlock> generateFocusProposal(
            int focusScore,
            String insight,
            String recommendedTask,
            List<CalendarEvent> events
    ) {
        String eventsSummary = events.stream()
                .map(event -> String.format("- [ ] %s (%s - %s)",
                        event.summary(),
                        event.startTime().format(TIME_FORMATTER),
                        event.endTime().format(TIME_FORMATTER)))
                .collect(Collectors.joining("\n"));

        final String checklist = eventsSummary.isEmpty() ? "현재 등록된 일정이 없습니다." : eventsSummary;

        return asBlocks(
            header(h -> h.text(plainText("🚀 집중 골든타임 알림", true))),
            section(s -> s.text(markdownText(String.format(
                    "*현재 집중 점수: %d점*\n%s", focusScore, insight)))),
            section(s -> s.text(markdownText(String.format(
                    "지금 바로 *[%s]* 업무를 시작해 보시는 건 어떨까요?\n\n*오늘의 체크리스트:*\n%s", 
                    recommendedTask, checklist)))),
            actions(a -> a.elements(asElements(
                    button(b -> b.text(plainText("지금 시작", true))
                            .style("primary")
                            .actionId("focus_start")),
                    button(b -> b.text(plainText("30분 뒤에", true))
                            .actionId("focus_delay")),
                    button(b -> b.text(plainText("안 할래", true))
                            .style("danger")
                            .actionId("focus_reject"))
            )))
        );
    }

    /**
     * 오늘의 Task 목록 메시지 템플릿
     * 각 태스크에 완료 버튼 포함 (action_id: task_complete_{taskId})
     */
    public List<LayoutBlock> generateTaskList(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return asBlocks(
                header(h -> h.text(plainText("📋 오늘의 할 일", true))),
                section(s -> s.text(markdownText("오늘 마감인 태스크가 없습니다.")))
            );
        }

        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("📋 오늘의 할 일", true))));

        for (Task task : tasks) {
            String status = task.completed() ? "✅" : "⬜";
            String dueText = task.dueAt() != null ? " `" + task.dueAt().format(DateTimeFormatter.ofPattern("HH:mm")) + "`" : "";
            String taskText = String.format("%s *%s*%s", status, task.title(), dueText);

            if (task.notes() != null && !task.notes().isBlank()) {
                taskText += "\n> " + task.notes();
            }

            final String finalTaskText = taskText;
            final String taskId = task.id();

            if (!task.completed() && taskId != null) {
                blocks.add(section(s -> s
                    .text(markdownText(finalTaskText))
                    .accessory(button(b -> b
                        .text(plainText("완료", true))
                        .style("primary")
                        .actionId("task_complete_" + taskId)
                    ))
                ));
            } else {
                blocks.add(section(s -> s.text(markdownText(finalTaskText))));
            }
        }

        return blocks;
    }

    /**
     * 비상 에너지 부스트 알림
     */
    public List<LayoutBlock> generateEmergencyBoost(int focusScore, String nextEvent) {
        return asBlocks(
            section(s -> s.text(markdownText(String.format(
                    "*🚨 비상 에너지 부스트 알림!*\n\n집중 점수 *%d점*으로 매우 낮습니다. 곧 *%s* 일정이 시작됩니다. 지금 바로 회복하세요!",
                    focusScore, nextEvent)))),
            actions(a -> a.elements(asElements(
                    button(b -> b.text(plainText("☕ 카페인 충전", true)).actionId("boost_coffee")),
                    button(b -> b.text(plainText("🤸 5분 스트레칭", true)).actionId("boost_stretch"))
            )))
        );
    }
}
