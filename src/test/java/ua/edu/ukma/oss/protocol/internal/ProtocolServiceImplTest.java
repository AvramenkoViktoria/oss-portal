package ua.edu.ukma.oss.protocol.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.protocol.ApprovalRequirementProvider;
import ua.edu.ukma.oss.protocol.FileStorage;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolPublishedEvent;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.protocol.Requirement;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

class ProtocolServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private final ProtocolRepository repo = mock(ProtocolRepository.class);
    private final FileStorage storage = mock(FileStorage.class);
    private final MeetingService meetings = mock(MeetingService.class);
    private final ApprovalRequirementProvider provider = mock(ApprovalRequirementProvider.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ProtocolServiceImpl service = new ProtocolServiceImpl(repo, storage, meetings, List.of(provider),
            events, Clock.fixed(NOW, ZoneOffset.UTC));

    private Protocol protocol(String fileKey) {
        Protocol p = new Protocol(3L, 1L, fileKey);
        ReflectionTestUtils.setField(p, "id", 10L);
        when(repo.findById(10L)).thenReturn(Optional.of(p));
        return p;
    }

    private static InputStream bytes() {
        return new ByteArrayInputStream(new byte[] {1});
    }

    @Test
    void readsByIdMeetingAndOrg() {
        Protocol p = protocol(null);
        when(repo.findByMeetingId(3L)).thenReturn(Optional.of(p));
        when(repo.findByOrgIdAndStatusOrderByPublishedAtDesc(1L, ProtocolStatus.PUBLISHED)).thenReturn(List.of(p));
        when(repo.findByOrgIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(p));

        assertThat(service.get(10L).meetingId()).isEqualTo(3L);
        assertThat(service.byMeeting(3L)).isPresent();
        assertThat(service.byMeeting(4L)).isEmpty();
        assertThat(service.published(1L)).hasSize(1);
        assertThat(service.all(1L)).hasSize(1);
        assertThatThrownBy(() -> service.get(11L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void requirementsStartWithTheDocumentAndIncludeProviders() {
        protocol(null);
        when(provider.requirementsFor(10L)).thenReturn(List.of(new Requirement("votes", "Голосування", true)));

        List<Requirement> requirements = service.approvalRequirements(10L);

        assertThat(requirements).extracting(Requirement::code).containsExactly("document", "votes");
        assertThat(requirements.getFirst().met()).isFalse();
    }

    @Test
    void fileIsEmptyUntilAttachedThenOpensFromStorage() {
        Protocol p = protocol(null);
        assertThat(service.file(10L)).isEmpty();

        p.attachFile("key", "protocol.pdf");
        when(storage.open("key")).thenReturn(bytes());

        assertThat(service.file(10L)).get().extracting(f -> f.fileName()).isEqualTo("protocol.pdf");
    }

    @Test
    void draftOnlyForHeldMeetingWithoutProtocol() {
        when(meetings.get(3L)).thenReturn(new MeetingDto(3L, 1L, "M", NOW, null, MeetingStatus.PLANNED, List.of()));
        assertThatThrownBy(() -> service.createDraft(3L, null)).isInstanceOf(BusinessRuleException.class);

        when(meetings.get(3L)).thenReturn(new MeetingDto(3L, 1L, "M", NOW, null, MeetingStatus.HELD, List.of()));
        when(repo.findByMeetingId(3L)).thenReturn(Optional.of(new Protocol(3L, 1L, null)));
        assertThatThrownBy(() -> service.createDraft(3L, null)).isInstanceOf(BusinessRuleException.class);

        when(repo.findByMeetingId(3L)).thenReturn(Optional.empty());
        when(repo.save(any(Protocol.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.createDraft(3L, null).status()).isEqualTo(ProtocolStatus.DRAFT);
    }

    @Test
    void heldMeetingEventCreatesDraft() {
        when(meetings.get(3L)).thenReturn(new MeetingDto(3L, 1L, "M", NOW, null, MeetingStatus.HELD, List.of()));
        when(repo.findByMeetingId(3L)).thenReturn(Optional.empty());
        when(repo.save(any(Protocol.class))).thenAnswer(inv -> inv.getArgument(0));

        service.onMeetingHeld(new MeetingHeldEvent(3L, 1L));

        verify(repo).save(any(Protocol.class));
    }

    @Test
    void attachFileChecksExtensionAndReplacesPreviousFile() {
        protocol("old");
        assertThatThrownBy(() -> service.attachFile(10L, "notes.txt", bytes())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.attachFile(10L, null, bytes())).isInstanceOf(BusinessRuleException.class);
        when(storage.store(any(), any())).thenReturn("new");

        ProtocolDto dto = service.attachFile(10L, "Protocol.PDF", bytes());

        assertThat(dto.fileKey()).isEqualTo("new");
        assertThat(dto.fileName()).isEqualTo("Protocol.PDF");
        verify(storage).delete("old");
    }

    @Test
    void approvalNeedsAllRequirementsThenPublishes() {
        Protocol p = protocol(null);
        assertThatThrownBy(() -> service.approve(10L, 7L))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Документ протоколу");

        p.attachFile("key", "p.pdf");
        assertThat(service.approve(10L, 7L).status()).isEqualTo(ProtocolStatus.APPROVED);
        assertThatThrownBy(() -> service.attachFile(10L, "p.pdf", bytes())).isInstanceOf(BusinessRuleException.class);

        ProtocolDto published = service.publish(10L, 7L);
        assertThat(published.status()).isEqualTo(ProtocolStatus.PUBLISHED);
        assertThat(published.publishedAt()).isEqualTo(NOW);
        verify(events).publishEvent(any(ProtocolPublishedEvent.class));
    }
}
