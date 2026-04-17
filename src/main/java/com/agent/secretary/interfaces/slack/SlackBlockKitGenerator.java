package com.agent.secretary.interfaces.slack;

import com.agent.secretary.domain.model.CalendarEvent;
import com.agent.secretary.domain.model.Task;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.view.View;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;

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
     * 일일 브리핑 메시지 Block Kit 생성.
     * block_id 컨벤션: task_card_{taskId}, task_actions_{taskId}, task_footer
     */
    public List<LayoutBlock> generateDailyBriefing(List<Task> tasks, java.time.LocalDate date) {
        String dateHeader = buildDateHeader(date);
        List<LayoutBlock> blocks = new ArrayList<>();

        blocks.add(header(h -> h.text(plainText("📋 오늘의 할 일 — " + dateHeader, true))));

        if (tasks.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText("오늘 등록된 할 일이 없습니다. 여유로운 하루 되세요 😊"))));
            blocks.add(buildFooterActions());
            return blocks;
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            final int order = i + 1;
            final String taskId = task.id();
            final String timeText = buildTimeText(task);
            final String cardText = String.format("*#%d*  %s\n%s", order, task.title(), timeText);

            blocks.add(section(s -> s
                .blockId("task_card_" + taskId)
                .text(markdownText(cardText))
            ));
            blocks.add(actions(a -> a
                .blockId("task_actions_" + taskId)
                .elements(asElements(
                    button(b -> b
                        .text(plainText("✏️ 수정", true))
                        .actionId("task_edit_" + taskId)
                    ),
                    button(b -> b
                        .text(plainText("✅ 완료", true))
                        .style("primary")
                        .actionId("task_complete_" + taskId)
                    )
                ))
            ));
            blocks.add(divider());
        }

        blocks.add(context(c -> c.elements(List.of(
            markdownText("총 " + tasks.size() + "개 태스크")
        ))));
        blocks.add(buildFooterActions());

        return blocks;
    }

    private String buildDateHeader(java.time.LocalDate date) {
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        String dayOfWeek = days[date.getDayOfWeek().getValue() - 1];
        return String.format("%d년 %d월 %d일 (%s)", date.getYear(), date.getMonthValue(), date.getDayOfMonth(), dayOfWeek);
    }

    private String buildTimeText(Task task) {
        if (task.dueAt() == null) {
            return "⏱ 시간 미정";
        }
        return "🕘 " + task.dueAt()
                .withOffsetSameInstant(java.time.ZoneOffset.ofHours(9))
                .format(TIME_FORMATTER);
    }

    public View buildTaskAddModal() {
        return view(v -> v
            .type("modal")
            .callbackId("task_add_modal")
            .title(viewTitle(t -> t.type("plain_text").text("태스크 추가")))
            .submit(viewSubmit(s -> s.type("plain_text").text("추가")))
            .close(viewClose(c -> c.type("plain_text").text("취소")))
            .blocks(asBlocks(
                input(i -> i
                    .blockId("title_block")
                    .label(plainText("제목"))
                    .element(plainTextInput(p -> p
                        .actionId("title_input")
                        .placeholder(plainText("태스크 제목을 입력하세요"))
                    ))
                ),
                input(i -> i
                    .blockId("notes_block")
                    .optional(true)
                    .label(plainText("메모"))
                    .element(plainTextInput(p -> p
                        .actionId("notes_input")
                        .multiline(true)
                        .placeholder(plainText("메모 (선택사항)"))
                    ))
                )
            ))
        );
    }

    public View buildTaskEditModal(Task task) {
        return view(v -> v
            .type("modal")
            .callbackId("task_edit_modal")
            .privateMetadata(task.id())
            .title(viewTitle(t -> t.type("plain_text").text("태스크 수정")))
            .submit(viewSubmit(s -> s.type("plain_text").text("저장")))
            .close(viewClose(c -> c.type("plain_text").text("취소")))
            .blocks(asBlocks(
                input(i -> i
                    .blockId("title_block")
                    .label(plainText("제목"))
                    .element(plainTextInput(p -> p
                        .actionId("title_input")
                        .initialValue(task.title())
                    ))
                ),
                input(i -> i
                    .blockId("notes_block")
                    .optional(true)
                    .label(plainText("메모"))
                    .element(plainTextInput(p -> p
                        .actionId("notes_input")
                        .multiline(true)
                        .initialValue(task.notes() != null ? task.notes() : "")
                    ))
                )
            ))
        );
    }

    private LayoutBlock buildFooterActions() {
        return actions(a -> a
            .blockId("task_footer")
            .elements(asElements(
                button(b -> b
                    .text(plainText("➕ 태스크 추가", true))
                    .actionId("task_add")
                )
            ))
        );
    }

    /**
     * 저녁 22시 체크인 메시지 Block Kit 생성.
     * 완료된 태스크는 취소선, 미완료 태스크는 checkin_complete_{taskId} 버튼 포함.
     */
    public List<LayoutBlock> generateEveningCheckIn(List<Task> tasks) {
        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(header(h -> h.text(plainText("🌙 오늘 하루 마무리 체크인", true))));
        blocks.add(section(s -> s.text(markdownText("아직 완료하지 않은 할 일이 있습니다. 확인해 주세요!"))));
        blocks.add(divider());

        for (Task task : tasks) {
            final String taskId = task.id();
            if (task.completed()) {
                blocks.add(section(s -> s.text(markdownText("✅  ~" + task.title() + "~"))));
            } else {
                blocks.add(section(s -> s
                    .text(markdownText("⬜  " + task.title()))
                    .accessory(button(b -> b
                        .text(plainText("✅ 완료", true))
                        .style("primary")
                        .actionId("checkin_complete_" + taskId)
                    ))
                ));
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
