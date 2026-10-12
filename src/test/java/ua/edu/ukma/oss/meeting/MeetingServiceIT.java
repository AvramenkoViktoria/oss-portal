package ua.edu.ukma.oss.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
@RecordApplicationEvents
class MeetingServiceIT {

    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    ApplicationEvents events;

    private Long sk() {
        return organizations.getByCode(OrgCode.SK).id();
    }

    private MeetingDto scheduleStartedMeeting() {
        return meetings.schedule(new ScheduleCmd(sk(), "Session", Instant.now().minus(1, ChronoUnit.HOURS), null));
    }

    @Test
    void schedulingPublishesAnnouncementEvent() {
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk(), "  Session  ",
                Instant.now().plus(1, ChronoUnit.DAYS), "https://stream.example/1"));

        assertThat(meeting.status()).isEqualTo(MeetingStatus.PLANNED);
        assertThat(meeting.title()).isEqualTo("Session");
        assertThat(events.stream(MeetingScheduledEvent.class)).singleElement()
                .extracting(MeetingScheduledEvent::meetingId).isEqualTo(meeting.id());
    }

    @Test
    void agendaItemsAreNumberedInOrder() {
        MeetingDto meeting = scheduleStartedMeeting();

        meetings.addAgendaItem(meeting.id(), "Budget");
        meetings.addAgendaItem(meeting.id(), "Elections");

        assertThat(meetings.get(meeting.id()).agenda())
                .extracting(AgendaItemDto::position, AgendaItemDto::title)
                .containsExactly(tuple(1, "Budget"),
                        tuple(2, "Elections"));
    }

    @Test
    void holdingPublishesEventAndFreezesAgenda() {
        MeetingDto meeting = scheduleStartedMeeting();

        MeetingDto held = meetings.markHeld(meeting.id());

        assertThat(held.status()).isEqualTo(MeetingStatus.HELD);
        assertThat(events.stream(MeetingHeldEvent.class)).singleElement()
                .extracting(MeetingHeldEvent::meetingId).isEqualTo(meeting.id());
        assertThatThrownBy(() -> meetings.addAgendaItem(meeting.id(), "Late item"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancellingPublishesEventAndCannotBeRepeated() {
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk(), "Session",
                Instant.now().plus(2, ChronoUnit.DAYS), null));

        meetings.cancel(meeting.id());

        assertThat(events.stream(MeetingCancelledEvent.class)).hasSize(1);
        assertThatThrownBy(() -> meetings.cancel(meeting.id())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listsMeetingsOfOneBodyNewestFirst() {
        Long ks = organizations.getByCode(OrgCode.KS).id();
        MeetingDto older = meetings.schedule(new ScheduleCmd(sk(), "Older", Instant.now().plus(1, ChronoUnit.DAYS), null));
        MeetingDto newer = meetings.schedule(new ScheduleCmd(sk(), "Newer", Instant.now().plus(5, ChronoUnit.DAYS), null));
        meetings.schedule(new ScheduleCmd(ks, "Other body", Instant.now().plus(3, ChronoUnit.DAYS), null));

        List<Long> ids = meetings.byOrg(sk()).stream().map(MeetingDto::id).toList();
        assertThat(ids).contains(newer.id(), older.id());
        assertThat(ids.indexOf(newer.id())).isLessThan(ids.indexOf(older.id()));
        assertThat(meetings.byOrg(sk())).extracting(MeetingDto::title).doesNotContain("Other body");
    }
}
