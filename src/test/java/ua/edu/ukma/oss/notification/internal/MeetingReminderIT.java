package ua.edu.ukma.oss.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.notification.NotificationAdminService;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class MeetingReminderIT {

    @Autowired
    MeetingReminderJob job;
    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    NotificationAdminService admin;

    private MeetingDto schedule(String title, long minutesAhead) {
        return meetings.schedule(new ScheduleCmd(organizations.getByCode(OrgCode.SK).id(), title,
                Instant.now().plus(minutesAhead, ChronoUnit.MINUTES), null));
    }

    private long reminders(String title) {
        return admin.recent(200).stream()
                .filter(e -> e.payload().contains("Нагадування") && e.payload().contains(title)).count();
    }

    @Test
    void remindsOnceWhenTheMeetingIsWithinAnHour() {
        String title = "Soon-" + UUID.randomUUID();
        schedule(title, 45);

        job.remindDue();
        job.remindDue();

        assertThat(reminders(title)).isEqualTo(1);
    }

    @Test
    void doesNotRemindAboutLaterOrCancelledMeetings() {
        String later = "Later-" + UUID.randomUUID();
        String cancelled = "Cancelled-" + UUID.randomUUID();
        schedule(later, 180);
        meetings.cancel(schedule(cancelled, 30).id());

        job.remindDue();

        assertThat(reminders(later)).isZero();
        assertThat(reminders(cancelled)).isZero();
    }

    @Test
    void meetingCreatedLessThanAnHourAheadIsRemindedRightAway() {
        String title = "Late-" + UUID.randomUUID();
        schedule(title, 10);

        job.remindDue();

        assertThat(reminders(title)).isEqualTo(1);
    }

    @Test
    void reschedulingAllowsAnotherReminder() {
        String title = "Moved-" + UUID.randomUUID();
        MeetingDto meeting = schedule(title, 30);
        job.remindDue();

        meetings.reschedule(meeting.id(), title, Instant.now().plus(20, ChronoUnit.MINUTES), null);
        job.remindDue();

        assertThat(reminders(title)).isEqualTo(2);
    }
}
