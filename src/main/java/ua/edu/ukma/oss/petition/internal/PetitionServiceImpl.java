package ua.edu.ukma.oss.petition.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingHeldEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.AuthorEligibilityPolicy;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.petition.SubmitCmd;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class PetitionServiceImpl implements PetitionService {

    private static final int MAX_TITLE_LENGTH = 255;

    private final PetitionRepository repo;
    private final AuthorEligibilityPolicy policy;
    private final MeetingService meetings;
    private final OrganizationService organizations;

    PetitionServiceImpl(PetitionRepository repo, AuthorEligibilityPolicy policy, MeetingService meetings,
                        OrganizationService organizations) {
        this.repo = repo;
        this.policy = policy;
        this.meetings = meetings;
        this.organizations = organizations;
    }

    @Override
    public PetitionDto submit(SubmitCmd cmd) {
        if (cmd.title() == null || cmd.title().isBlank() || cmd.title().length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleException("Title is required and must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        if (cmd.text() == null || cmd.text().isBlank()) {
            throw new BusinessRuleException("Petition text is required");
        }
        organizations.get(cmd.orgId());
        policy.ensureEligible(cmd.authorId(), cmd.orgId());
        return toDto(repo.save(new Petition(cmd.title().trim(), cmd.text().trim(), cmd.authorId(), cmd.orgId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PetitionDto get(Long petitionId) {
        return toDto(find(petitionId));
    }

    @Override
    public PetitionDto attach(Long petitionId, Long meetingId, PetitionLink link) {
        Petition petition = find(petitionId);
        MeetingDto meeting = meetings.get(meetingId);
        if (!meeting.orgId().equals(petition.getOrgId())) {
            throw new BusinessRuleException("The meeting belongs to another body than the petition");
        }
        if (meeting.status() != MeetingStatus.PLANNED) {
            throw new BusinessRuleException("A petition can only be scheduled to a planned meeting");
        }
        petition.schedule(meetingId, link);
        if (link == PetitionLink.AGENDA_ITEM) {
            meetings.addAgendaItem(meetingId, "Petition: " + petition.getTitle());
        }
        return toDto(petition);
    }

    @Override
    public PetitionDto scheduleSeparateMeeting(Long petitionId, String meetingTitle, Instant scheduledAt,
                                               String streamUrl) {
        Petition petition = find(petitionId);
        if (petition.getStatus() != PetitionStatus.SUBMITTED) {
            throw new BusinessRuleException("A petition that is " + petition.getStatus().name().toLowerCase()
                    + " cannot be scheduled");
        }
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(petition.getOrgId(), meetingTitle, scheduledAt, streamUrl));
        meetings.addAgendaItem(meeting.id(), "Petition: " + petition.getTitle());
        petition.schedule(meeting.id(), PetitionLink.SEPARATE_MEETING);
        return toDto(petition);
    }

    @Override
    public PetitionDto reject(Long petitionId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A rejection reason is required");
        }
        Petition petition = find(petitionId);
        petition.reject(reason.trim());
        return toDto(petition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetitionDto> byOrg(Long orgId) {
        return repo.findByOrgIdOrderByCreatedAtDesc(orgId).stream().map(PetitionServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetitionDto> byAuthor(Long authorId) {
        return repo.findByAuthorIdOrderByCreatedAtDesc(authorId).stream().map(PetitionServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetitionDto> byMeeting(Long meetingId) {
        return repo.findByMeetingIdOrderByCreatedAt(meetingId).stream().map(PetitionServiceImpl::toDto).toList();
    }

    /**
     * Runs in the transaction that marks the meeting as held, so both changes commit together.
     */
    @EventListener
    void onMeetingHeld(MeetingHeldEvent event) {
        repo.findByMeetingIdAndStatus(event.meetingId(), PetitionStatus.SCHEDULED).forEach(Petition::consider);
    }

    @EventListener
    void onMeetingCancelled(MeetingCancelledEvent event) {
        repo.findByMeetingIdAndStatus(event.meetingId(), PetitionStatus.SCHEDULED).forEach(Petition::unschedule);
    }

    private Petition find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Petition", id));
    }

    private static PetitionDto toDto(Petition p) {
        return new PetitionDto(p.getId(), p.getTitle(), p.getText(), p.getAuthorId(), p.getOrgId(), p.getStatus(),
                p.getLink(), p.getMeetingId(), p.getRejectionReason(), p.getCreatedAt());
    }
}
