package ua.edu.ukma.oss.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class AnnouncementsIT {

    @Autowired
    NotificationAdminService admin;
    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    ProtocolReader reader;
    @Autowired
    ProtocolWriter writer;
    @Autowired
    UserService users;

    private boolean queued(String fragment) {
        return admin.recent(100).stream().anyMatch(e -> e.payload().contains(fragment));
    }

    @Test
    void schedulingAnnouncesTheMeeting() {
        String title = "Announce-" + UUID.randomUUID();

        meetings.schedule(new ScheduleCmd(organizations.getByCode(OrgCode.SK).id(), title,
                Instant.now().plus(2, ChronoUnit.DAYS), "https://stream.example/live"));

        assertThat(admin.recent(100)).anyMatch(e -> e.payload().contains(title)
                && e.payload().contains("https://stream.example/live"));
    }

    @Test
    void cancellationIsAnnounced() {
        String title = "Cancel-" + UUID.randomUUID();
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(organizations.getByCode(OrgCode.KS).id(), title,
                Instant.now().plus(2, ChronoUnit.DAYS), null));

        meetings.cancel(meeting.id());

        assertThat(queued("скасовано")).isTrue();
    }

    @Test
    void publishingProtocolQueuesLinkToTheMeeting() {
        String title = "Protocol-" + UUID.randomUUID();
        Long sk = organizations.getByCode(OrgCode.SK).id();
        Long actor = users.register("p-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Editor").id();
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk, title, Instant.now().minus(1, ChronoUnit.HOURS), null));
        meetings.markHeld(meeting.id());
        ProtocolDto protocol = reader.byMeeting(meeting.id()).orElseThrow();
        writer.attachFile(protocol.id(), "minutes.pdf", new ByteArrayInputStream(new byte[] {1}));
        writer.approve(protocol.id(), actor);

        writer.publish(protocol.id(), actor);

        assertThat(admin.recent(100)).anyMatch(e -> e.payload().contains(title)
                && e.payload().contains("/meetings/" + meeting.id()) && e.orgId().equals(sk));
    }
}
