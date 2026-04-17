package com.agent.secretary.interfaces.slack;

import com.agent.secretary.domain.model.Task;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.ButtonElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlackBlockKitGeneratorTest {

    private SlackBlockKitGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SlackBlockKitGenerator();
    }

    @Test
    @DisplayName("태스크 없는 날 — 헤더 + 빈 상태 섹션 + 푸터 액션 블록 반환")
    void generateDailyBriefing_emptyTasks() {
        LocalDate date = LocalDate.of(2026, 4, 17);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(), date);

        assertThat(blocks).hasSize(3);
        assertThat(blocks.get(0)).isInstanceOf(HeaderBlock.class);
        assertThat(blocks.get(1)).isInstanceOf(SectionBlock.class);
        assertThat(blocks.get(2)).isInstanceOf(ActionsBlock.class);

        ActionsBlock footer = (ActionsBlock) blocks.get(2);
        assertThat(footer.getBlockId()).isEqualTo("task_footer");
        ButtonElement addBtn = (ButtonElement) footer.getElements().get(0);
        assertThat(addBtn.getActionId()).isEqualTo("task_add");
    }

    @Test
    @DisplayName("태스크 1개 — 헤더 + 섹션카드 + 액션카드 + 구분선 + 컨텍스트 + 푸터 = 6블록")
    void generateDailyBriefing_singleTask() {
        LocalDate date = LocalDate.of(2026, 4, 17);
        Task task = new Task("task-1", "등산 준비하기", null, false,
                OffsetDateTime.of(2026, 4, 17, 9, 0, 0, 0, ZoneOffset.ofHours(9)), null);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(task), date);

        // Header + SectionCard + ActionsCard + Divider + Context + FooterActions
        assertThat(blocks).hasSize(6);
        assertThat(blocks.get(0)).isInstanceOf(HeaderBlock.class);
        assertThat(blocks.get(1)).isInstanceOf(SectionBlock.class);
        assertThat(blocks.get(2)).isInstanceOf(ActionsBlock.class);
        assertThat(blocks.get(3)).isInstanceOf(DividerBlock.class);
        assertThat(blocks.get(4)).isInstanceOf(ContextBlock.class);
        assertThat(blocks.get(5)).isInstanceOf(ActionsBlock.class);
    }

    @Test
    @DisplayName("태스크 섹션 block_id = task_card_{taskId}")
    void generateDailyBriefing_blockIdConvention() {
        LocalDate date = LocalDate.of(2026, 4, 17);
        Task task = new Task("abc-123", "테스트 태스크", null, false, null, null);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(task), date);

        SectionBlock card = (SectionBlock) blocks.get(1);
        assertThat(card.getBlockId()).isEqualTo("task_card_abc-123");

        ActionsBlock actions = (ActionsBlock) blocks.get(2);
        assertThat(actions.getBlockId()).isEqualTo("task_actions_abc-123");
    }

    @Test
    @DisplayName("태스크 액션 — 수정 버튼 action_id=task_edit_{id}, 완료 버튼 action_id=task_complete_{id}")
    void generateDailyBriefing_actionIds() {
        Task task = new Task("xyz-9", "할 일", null, false, null, null);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(task), LocalDate.now());

        ActionsBlock actions = (ActionsBlock) blocks.get(2);
        ButtonElement editBtn = (ButtonElement) actions.getElements().get(0);
        ButtonElement completeBtn = (ButtonElement) actions.getElements().get(1);

        assertThat(editBtn.getActionId()).isEqualTo("task_edit_xyz-9");
        assertThat(completeBtn.getActionId()).isEqualTo("task_complete_xyz-9");
    }

    @Test
    @DisplayName("dueAt 있을 때 시간 표시 포함")
    void generateDailyBriefing_withDueAt_showsTime() {
        Task task = new Task("t1", "치킨먹기", null, false,
                OffsetDateTime.of(2026, 4, 17, 22, 0, 0, 0, ZoneOffset.ofHours(9)), null);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(task), LocalDate.now());

        SectionBlock card = (SectionBlock) blocks.get(1);
        String text = card.getText().getText();
        assertThat(text).contains("22:00");
        assertThat(text).doesNotContain("시간 미정");
    }

    @Test
    @DisplayName("dueAt 없을 때 '시간 미정' 표시")
    void generateDailyBriefing_noDueAt_showsNoTime() {
        Task task = new Task("t2", "자전거 타기", null, false, null, null);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(task), LocalDate.now());

        SectionBlock card = (SectionBlock) blocks.get(1);
        String text = card.getText().getText();
        assertThat(text).contains("시간 미정");
    }

    @Test
    @DisplayName("날짜 헤더에 날짜 포함")
    void generateDailyBriefing_headerContainsDate() {
        LocalDate date = LocalDate.of(2026, 4, 17);

        List<LayoutBlock> blocks = generator.generateDailyBriefing(List.of(), date);

        HeaderBlock header = (HeaderBlock) blocks.get(0);
        assertThat(header.getText().getText()).contains("2026");
        assertThat(header.getText().getText()).contains("4");
        assertThat(header.getText().getText()).contains("17");
    }
}
