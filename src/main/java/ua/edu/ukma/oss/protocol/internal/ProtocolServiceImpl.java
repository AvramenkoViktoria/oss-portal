package ua.edu.ukma.oss.protocol.internal;

import java.io.InputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.protocol.ApprovalRequirementProvider;
import ua.edu.ukma.oss.protocol.FileStorage;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolFile;
import ua.edu.ukma.oss.protocol.ProtocolPublishedEvent;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.protocol.Requirement;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class ProtocolServiceImpl implements ProtocolReader, ProtocolWriter {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".docx");

    private final ProtocolRepository repo;
    private final FileStorage storage;
    private final MeetingService meetings;
    private final List<ApprovalRequirementProvider> providers;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ProtocolServiceImpl(ProtocolRepository repo, FileStorage storage, MeetingService meetings,
                        List<ApprovalRequirementProvider> providers, ApplicationEventPublisher events, Clock clock) {
        this.repo = repo;
        this.storage = storage;
        this.meetings = meetings;
        this.providers = providers;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProtocolDto get(Long protocolId) {
        return toDto(find(protocolId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProtocolDto> byMeeting(Long meetingId) {
        return repo.findByMeetingId(meetingId).map(ProtocolServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProtocolDto> published(Long orgId) {
        return repo.findByOrgIdAndStatusOrderByPublishedAtDesc(orgId, ProtocolStatus.PUBLISHED).stream()
                .map(ProtocolServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProtocolDto> all(Long orgId) {
        return repo.findByOrgIdOrderByCreatedAtDesc(orgId).stream().map(ProtocolServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Requirement> approvalRequirements(Long protocolId) {
        Protocol protocol = find(protocolId);
        List<Requirement> all = new ArrayList<>();
        all.add(new Requirement("document", "Документ протоколу (PDF або DOCX) прикріплено", protocol.hasFile()));
        providers.forEach(p -> all.addAll(p.requirementsFor(protocolId)));
        return all;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProtocolFile> file(Long protocolId) {
        Protocol protocol = find(protocolId);
        if (!protocol.hasFile()) {
            return Optional.empty();
        }
        return Optional.of(new ProtocolFile(protocol.getFileName(), storage.open(protocol.getFileKey())));
    }

    @Override
    public ProtocolDto createDraft(Long meetingId, String fileKey) {
        MeetingDto meeting = meetings.get(meetingId);
        if (meeting.status() != MeetingStatus.HELD) {
            throw new BusinessRuleException("Протокол можна створити лише для проведеного засідання");
        }
        if (repo.findByMeetingId(meetingId).isPresent()) {
            throw new BusinessRuleException("У цього засідання вже є протокол");
        }
        return toDto(repo.save(new Protocol(meetingId, meeting.orgId(), fileKey)));
    }

    @Override
    public ProtocolDto attachFile(Long protocolId, String fileName, InputStream content) {
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException("Приймаються лише документи PDF та DOCX");
        }
        Protocol protocol = find(protocolId);
        if (!protocol.isEditable()) {
            throw new BusinessRuleException("Документ затвердженого чи опублікованого протоколу змінити неможливо");
        }
        String key = storage.store(content, fileName);
        String previous = protocol.attachFile(key, fileName);
        if (previous != null) {
            storage.delete(previous);
        }
        return toDto(protocol);
    }

    @Override
    public ProtocolDto approve(Long protocolId, Long actorId) {
        Protocol protocol = find(protocolId);
        List<String> unmet = approvalRequirements(protocolId).stream()
                .filter(r -> !r.met()).map(Requirement::description).toList();
        if (!unmet.isEmpty()) {
            throw new BusinessRuleException("Затвердити поки неможливо: " + String.join("; ", unmet));
        }
        protocol.approve(actorId, clock.instant());
        return toDto(protocol);
    }

    @Override
    public ProtocolDto publish(Long protocolId, Long actorId) {
        Protocol protocol = find(protocolId);
        protocol.publish(actorId, clock.instant());
        events.publishEvent(new ProtocolPublishedEvent(protocol.getId(), protocol.getOrgId(), protocol.getMeetingId()));
        return toDto(protocol);
    }

    /**
     * A held meeting immediately gets its draft, in the same transaction as the status change.
     */
    @EventListener
    void onMeetingHeld(MeetingHeldEvent event) {
        createDraft(event.meetingId(), null);
    }

    private Protocol find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Protocol", id));
    }

    private static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static ProtocolDto toDto(Protocol p) {
        return new ProtocolDto(p.getId(), p.getMeetingId(), p.getOrgId(), p.getStatus(), p.getFileKey(),
                p.getFileName(), p.getCreatedAt(), p.getApprovedAt(), p.getApprovedBy(), p.getPublishedAt(),
                p.getPublishedBy());
    }
}
