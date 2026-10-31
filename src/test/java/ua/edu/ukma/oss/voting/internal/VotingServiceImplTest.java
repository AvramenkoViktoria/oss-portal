package ua.edu.ukma.oss.voting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.support.Docx;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteRecordDto;
import ua.edu.ukma.oss.voting.VoteResult;
import ua.edu.ukma.oss.voting.VotingDto;

class VotingServiceImplTest {

    private final VotingRepository votings = mock(VotingRepository.class);
    private final VoteRecordRepository records = mock(VoteRecordRepository.class);
    private final ProtocolReader protocols = mock(ProtocolReader.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final UserService users = mock(UserService.class);
    private final VotingServiceImpl service = new VotingServiceImpl(votings, records, protocols, organizations, users);

    private static final String[] TOTALS = {
            "Слухали:", "Про затвердження порядку денного.", "Поставили на голосування:",
            "Затвердити порядок денний засідання.", "Результати голосування:", "За - 2", "Проти - 0",
            "Утримався - 0", "Не голосували - 0", "Рішення прийнято."};

    private void protocol(ProtocolStatus status) {
        when(protocols.get(10L)).thenReturn(new ProtocolDto(10L, 3L, 1L, status, null, null, Instant.now(), null,
                null, null, null));
        when(votings.save(any(Voting.class))).thenAnswer(inv -> {
            Voting v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 50L);
            return v;
        });
    }

    @Test
    void importsTotalsFromDocxAndReplacesPreviousVotings() {
        protocol(ProtocolStatus.DRAFT);
        Voting old = mock(Voting.class);
        when(old.getId()).thenReturn(40L);
        when(votings.findByProtocolIdOrderById(10L)).thenReturn(List.of(old));

        List<VotingDto> result = service.importFromDocument(10L, "p.docx", Docx.of(TOTALS));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().votesFor()).isEqualTo(2);
        assertThat(result.getFirst().result()).isEqualTo(VoteResult.ACCEPTED);
        verify(records).deleteByVotingId(40L);
        verify(votings).delete(old);
    }

    @Test
    void importsNamedVotesAgainstTheBodyComposition() {
        protocol(ProtocolStatus.DRAFT);
        when(votings.findByProtocolIdOrderById(10L)).thenReturn(List.of());
        when(organizations.history(1L)).thenReturn(List.of(
                new MembershipDto(7L, 100L, 1L, "FI", "Член", LocalDate.of(2026, 1, 1), null),
                new MembershipDto(8L, 101L, 1L, "FI", "Член", LocalDate.of(2026, 1, 1), null)));
        when(users.fullNames(any())).thenReturn(Map.of(100L, "Іваненко Іван", 101L, "Петренко Петро"));

        service.importFromDocument(10L, "p.docx", Docx.of("Слухали:", "Про порядок.", "Поставили на голосування:",
                "Затвердити порядок.", "Результати голосування:", "За - 2: Іваненко Іван; Петренко Петро",
                "Проти - 0", "Утримався - 0", "Не голосували - 0"));

        verify(records, org.mockito.Mockito.times(2)).save(any(VoteRecord.class));
    }

    @Test
    void unknownVoterIsRejected() {
        protocol(ProtocolStatus.DRAFT);
        when(votings.findByProtocolIdOrderById(10L)).thenReturn(List.of());
        when(organizations.history(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.importFromDocument(10L, "p.docx", Docx.of("Слухали:", "Про порядок.",
                "Поставили на голосування:", "Затвердити порядок.", "Результати голосування:",
                "За - 1: Сторонній Хтось", "Проти - 0", "Утримався - 0", "Не голосували - 0")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("немає у складі");
    }

    @Test
    void importOnlyWhileProtocolIsDraftAndDocumentIsReadable() {
        protocol(ProtocolStatus.APPROVED);
        assertThatThrownBy(() -> service.importFromDocument(10L, "p.docx", Docx.of(TOTALS)))
                .isInstanceOf(BusinessRuleException.class);

        protocol(ProtocolStatus.DRAFT);
        assertThatThrownBy(() -> service.importFromDocument(10L, "p.docx", new byte[] {1, 2}))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("пошкоджений");
        assertThatThrownBy(() -> service.importFromDocument(10L, "p.txt", new byte[] {1}))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.importFromDocument(10L, null, new byte[] {1}))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void readsVotingsAndRecords() {
        Voting voting = new Voting(10L, ProtocolVotingParser.parse(String.join("\n", TOTALS)).getFirst());
        when(votings.findById(50L)).thenReturn(Optional.of(voting));
        when(votings.findByProtocolIdOrderById(10L)).thenReturn(List.of(voting));
        VoteRecord record = new VoteRecord(50L, 7L, VoteChoice.FOR);
        when(records.findByVotingIdOrderByMembershipId(50L)).thenReturn(List.of(record));

        assertThat(service.get(50L).question()).contains("порядок денний");
        assertThat(service.byProtocol(10L)).hasSize(1);
        assertThat(service.records(50L)).containsExactly(new VoteRecordDto(7L, VoteChoice.FOR));
        assertThatThrownBy(() -> service.get(51L)).isInstanceOf(NotFoundException.class);
    }
}
