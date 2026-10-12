package ua.edu.ukma.oss.petition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class PetitionServiceIT {

    @Autowired
    PetitionService petitions;
    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    UserService users;

    Long sk;
    Long student;

    @BeforeEach
    void setUp() {
        sk = organizations.getByCode(OrgCode.SK).id();
        student = newUser();
    }

    private Long newUser() {
        return users.register("u-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Student").id();
    }

    private PetitionDto submit() {
        return petitions.submit(new SubmitCmd(student, sk, "Longer library hours", "Keep it open until midnight"));
    }

    private MeetingDto plannedMeeting() {
        return meetings.schedule(new ScheduleCmd(sk, "Session", Instant.now().plus(1, ChronoUnit.DAYS), null));
    }

    @Test
    void studentSubmitsPetition() {
        PetitionDto petition = submit();

        assertThat(petition.status()).isEqualTo(PetitionStatus.SUBMITTED);
        assertThat(petitions.byAuthor(student)).extracting(PetitionDto::id).containsExactly(petition.id());
        assertThat(petitions.byOrg(sk)).extracting(PetitionDto::id).contains(petition.id());
    }

    @Test
    void memberOfAnyBodyCannotSubmit() {
        Long member = newUser();
        organizations.addMember(new AddMemberCmd(member, organizations.getByCode(OrgCode.KS).id(), "FI",
                "Delegate", LocalDate.now().minusDays(1), null));

        assertThatThrownBy(() -> petitions.submit(new SubmitCmd(member, sk, "Title", "Text")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void attachingAsAgendaItemAddsItemToMeeting() {
        PetitionDto petition = submit();
        MeetingDto meeting = plannedMeeting();

        PetitionDto scheduled = petitions.attach(petition.id(), meeting.id(), PetitionLink.AGENDA_ITEM);

        assertThat(scheduled.status()).isEqualTo(PetitionStatus.SCHEDULED);
        assertThat(meetings.get(meeting.id()).agenda()).singleElement()
                .satisfies(item -> assertThat(item.title()).contains("Longer library hours"));
    }

    @Test
    void separateMeetingIsCreatedForThePetition() {
        PetitionDto petition = submit();

        PetitionDto scheduled = petitions.scheduleSeparateMeeting(petition.id(), "Library hours",
                Instant.now().plus(3, ChronoUnit.DAYS), null);

        assertThat(scheduled.link()).isEqualTo(PetitionLink.SEPARATE_MEETING);
        assertThat(meetings.get(scheduled.meetingId()).orgId()).isEqualTo(sk);
    }

    @Test
    void holdingTheMeetingMarksPetitionConsidered() {
        PetitionDto petition = submit();
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk, "Session", Instant.now().minus(1, ChronoUnit.HOURS), null));
        petitions.attach(petition.id(), meeting.id(), PetitionLink.AGENDA_ITEM);

        meetings.markHeld(meeting.id());

        assertThat(petitions.get(petition.id()).status()).isEqualTo(PetitionStatus.CONSIDERED);
    }

    @Test
    void cancellingTheMeetingReturnsPetitionToQueue() {
        PetitionDto petition = submit();
        MeetingDto meeting = plannedMeeting();
        petitions.attach(petition.id(), meeting.id(), PetitionLink.AGENDA_ITEM);

        meetings.cancel(meeting.id());

        PetitionDto after = petitions.get(petition.id());
        assertThat(after.status()).isEqualTo(PetitionStatus.SUBMITTED);
        assertThat(after.meetingId()).isNull();
    }

    @Test
    void cannotAttachToMeetingOfAnotherBody() {
        PetitionDto petition = submit();
        MeetingDto other = meetings.schedule(new ScheduleCmd(organizations.getByCode(OrgCode.KS).id(), "KS session",
                Instant.now().plus(1, ChronoUnit.DAYS), null));

        assertThatThrownBy(() -> petitions.attach(petition.id(), other.id(), PetitionLink.AGENDA_ITEM))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectionRequiresReasonAndIsFinal() {
        PetitionDto petition = submit();

        assertThatThrownBy(() -> petitions.reject(petition.id(), " ")).isInstanceOf(BusinessRuleException.class);
        PetitionDto rejected = petitions.reject(petition.id(), "Outside the competence of the body");

        assertThat(rejected.status()).isEqualTo(PetitionStatus.REJECTED);
        assertThatThrownBy(() -> petitions.attach(petition.id(), plannedMeeting().id(), PetitionLink.AGENDA_ITEM))
                .isInstanceOf(BusinessRuleException.class);
    }
}
