package ua.edu.ukma.oss.voting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.voting.VoteResult;

class ProtocolVotingParserTest {

    @Test
    void readsShortSkFormatWithQuestionAfterPutToVote() {
        String text = """
                Слухали:
                Вішевника з пропозицією вилучити питання.
                Поставили на голосування:
                Вилучення з порядку денного питання про переобрання заступника голови СК.
                Результати голосування:
                5 — за.
                Рішення прийнято одноголосно.
                Вирішили:
                Вилучити питання.
                """;

        List<ParsedVoting> votings = ProtocolVotingParser.parse(text);

        assertThat(votings).hasSize(1);
        ParsedVoting v = votings.getFirst();
        assertThat(v.question()).isEqualTo("Вилучення з порядку денного питання про переобрання заступника голови СК");
        assertThat(v.votesFor()).isEqualTo(5);
        assertThat(v.votesAgainst()).isZero();
        assertThat(v.present()).isEqualTo(5);
        assertThat(v.result()).isEqualTo(VoteResult.ACCEPTED);
    }

    @Test
    void readsNumberedKsFormatWithTotalsAcrossPageBreak() {
        String text = """
                3) Слухали: Горобець Ганну щодо пункту про затвердження порядку денного засідання.
                Запитує, чи будуть пропозиції до порядку денного.
                Присутніх делегатів на момент прийняття рішення: 12 осіб.
                Результати голосування:

                2/9 - Протокол засідання КС НаУКМА || 12.10.2026 || №18/ХII || м. Київ
                За - 10
                Проти - 0
                Утримався - 0
                Не голосували - 2
                Рішення прийнято
                —
                Затвердили запропонований порядок денний засідання.
                6) Слухали: Горобець Ганну щодо пункту про Ухвалення Положення про самоврядування. Виступи.
                Присутніх делегатів на момент прийняття рішення: 17 осіб.
                Результати голосування:
                За - 15
                Проти - 2
                Утримався - 0
                Не голосували — 0
                Рішення не прийнято — Положення не ухвалено.
                """;

        List<ParsedVoting> votings = ProtocolVotingParser.parse(text);

        assertThat(votings).hasSize(2);
        assertThat(votings.get(0).question()).isEqualTo("Затвердження порядку денного засідання");
        assertThat(votings.get(0)).extracting(ParsedVoting::votesFor, ParsedVoting::votesAbsent, ParsedVoting::present)
                .containsExactly(10, 2, 12);
        assertThat(votings.get(1).question()).isEqualTo("Ухвалення Положення про самоврядування");
        assertThat(votings.get(1)).extracting(ParsedVoting::votesFor, ParsedVoting::votesAgainst, ParsedVoting::present)
                .containsExactly(15, 2, 17);
        assertThat(votings.get(1).result()).isEqualTo(VoteResult.REJECTED);
    }

    @Test
    void rejectsProtocolWithoutVotingSection() {
        assertThatThrownBy(() -> ProtocolVotingParser.parse("Слухали: звіт.\nЗасідання завершено."))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("розділу з результатами голосування");
    }

    @Test
    void rejectsSectionWithoutTotals() {
        assertThatThrownBy(() -> ProtocolVotingParser.parse("Поставили на голосування: питання.\nРезультати голосування: так.\nЗавершено."))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("кількість голосів");
    }

    @Test
    void readsNamesListedAfterTheTotals() {
        String text = """
                Поставили на голосування:
                Бюджет.
                Результати голосування:
                За - 2: Олена Коваленко; Андрій Шевченко
                Проти - 1: Іван Мельник.
                Утримався - 0
                Не голосували - 0
                Рішення прийнято
                """;

        ParsedVoting v = ProtocolVotingParser.parse(text).getFirst();

        assertThat(v.votesFor()).isEqualTo(2);
        assertThat(v.named()).containsExactly(
                java.util.Map.entry("Олена Коваленко", ua.edu.ukma.oss.voting.VoteChoice.FOR),
                java.util.Map.entry("Андрій Шевченко", ua.edu.ukma.oss.voting.VoteChoice.FOR),
                java.util.Map.entry("Іван Мельник", ua.edu.ukma.oss.voting.VoteChoice.AGAINST));
    }

    @Test
    void rejectsNamesThatDoNotMatchTheTotal() {
        assertThatThrownBy(() -> ProtocolVotingParser.parse(
                "Результати голосування: За - 3: Олена Коваленко; Андрій Шевченко Рішення прийнято"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("імен перелічено 2");
    }

    @Test
    void totalsOnlyProtocolsHaveNoNames() {
        assertThat(ProtocolVotingParser.parse("Результати голосування: 5 — за. Рішення прийнято").getFirst().named())
                .isEmpty();
    }
}
