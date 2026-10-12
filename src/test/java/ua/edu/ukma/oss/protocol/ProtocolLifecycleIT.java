package ua.edu.ukma.oss.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
@RecordApplicationEvents
class ProtocolLifecycleIT {

    @Autowired
    ProtocolReader reader;
    @Autowired
    ProtocolWriter writer;
    @Autowired
    MeetingService meetings;
    @Autowired
    OrganizationService organizations;
    @Autowired
    UserService users;
    @Autowired
    ApplicationEvents events;

    Long actor;
    Long sk;

    @BeforeEach
    void setUp() {
        sk = organizations.getByCode(OrgCode.SK).id();
        actor = users.register("a-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Editor").id();
    }

    private ProtocolDto heldMeetingProtocol() {
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk, "Session", Instant.now().minus(1, ChronoUnit.HOURS), null));
        meetings.markHeld(meeting.id());
        return reader.byMeeting(meeting.id()).orElseThrow();
    }

    private static ByteArrayInputStream content() {
        return new ByteArrayInputStream("%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void holdingTheMeetingCreatesDraftAutomatically() {
        ProtocolDto protocol = heldMeetingProtocol();

        assertThat(protocol.status()).isEqualTo(ProtocolStatus.DRAFT);
        assertThat(protocol.hasFile()).isFalse();
        assertThat(protocol.orgId()).isEqualTo(sk);
    }

    @Test
    void draftCannotBeCreatedForPlannedMeeting() {
        MeetingDto planned = meetings.schedule(new ScheduleCmd(sk, "Later", Instant.now().plus(1, ChronoUnit.DAYS), null));

        assertThatThrownBy(() -> writer.createDraft(planned.id(), null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void approvalIsBlockedUntilDocumentIsAttached() {
        ProtocolDto protocol = heldMeetingProtocol();

        assertThat(reader.approvalRequirements(protocol.id())).anyMatch(r -> r.code().equals("document") && !r.met());
        assertThatThrownBy(() -> writer.approve(protocol.id(), actor)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void onlyPdfAndDocxAreAccepted() {
        ProtocolDto protocol = heldMeetingProtocol();

        assertThatThrownBy(() -> writer.attachFile(protocol.id(), "notes.exe", content()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void fullLifecyclePublishesEventAndExposesProtocolPublicly() throws IOException {
        ProtocolDto protocol = heldMeetingProtocol();
        writer.attachFile(protocol.id(), "minutes.pdf", content());

        ProtocolDto approved = writer.approve(protocol.id(), actor);
        assertThat(approved.status()).isEqualTo(ProtocolStatus.APPROVED);
        assertThat(reader.published(sk)).extracting(ProtocolDto::id).doesNotContain(protocol.id());

        ProtocolDto published = writer.publish(protocol.id(), actor);

        assertThat(published.status()).isEqualTo(ProtocolStatus.PUBLISHED);
        assertThat(published.publishedBy()).isEqualTo(actor);
        assertThat(reader.published(sk)).extracting(ProtocolDto::id).contains(protocol.id());
        assertThat(events.stream(ProtocolPublishedEvent.class)).singleElement()
                .extracting(ProtocolPublishedEvent::protocolId).isEqualTo(protocol.id());
        try (var file = reader.file(protocol.id()).orElseThrow().content()) {
            assertThat(new String(file.readAllBytes(), StandardCharsets.UTF_8)).startsWith("%PDF");
        }
    }

    @Test
    void publishedProtocolIsImmutable() {
        ProtocolDto protocol = heldMeetingProtocol();
        writer.attachFile(protocol.id(), "minutes.pdf", content());
        writer.approve(protocol.id(), actor);
        writer.publish(protocol.id(), actor);

        assertThatThrownBy(() -> writer.attachFile(protocol.id(), "other.pdf", content()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> writer.publish(protocol.id(), actor)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotPublishWithoutApproval() {
        ProtocolDto protocol = heldMeetingProtocol();
        writer.attachFile(protocol.id(), "minutes.pdf", content());

        assertThatThrownBy(() -> writer.publish(protocol.id(), actor)).isInstanceOf(BusinessRuleException.class);
    }
}
