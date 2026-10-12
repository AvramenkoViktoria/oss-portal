package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolDto;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolWriter;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.support.IntegrationTest;

/**
 * Not transactional: MockMvc requests run in their own transactions and must see committed data.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PublicSiteIT {

    @Autowired
    MockMvc mvc;
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

    private ProtocolDto heldMeetingProtocol(String title) {
        Long sk = organizations.getByCode(OrgCode.SK).id();
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk, title, Instant.now().minus(1, ChronoUnit.HOURS), null));
        meetings.markHeld(meeting.id());
        return reader.byMeeting(meeting.id()).orElseThrow();
    }

    @Test
    void homeAndBodyPagesRenderForAnonymousVisitors() throws Exception {
        String home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(home).contains("Студентська колегія").contains("Конгрегація студентів");
        mvc.perform(get("/orgs/sk")).andExpect(status().isOk());
        mvc.perform(get("/orgs/ks")).andExpect(status().isOk());
    }

    @Test
    void unknownBodyIsNotFound() throws Exception {
        mvc.perform(get("/orgs/xx")).andExpect(status().isNotFound());
    }

    @Test
    void draftProtocolIsHiddenFromThePublic() throws Exception {
        ProtocolDto draft = heldMeetingProtocol("Hidden-" + UUID.randomUUID());

        mvc.perform(get("/protocols/" + draft.id())).andExpect(status().isNotFound());
        mvc.perform(get("/protocols/" + draft.id() + "/file")).andExpect(status().isNotFound());
    }

    @Test
    void publishedProtocolIsPublicWithItsDocument() throws Exception {
        String title = "Visible-" + UUID.randomUUID();
        ProtocolDto protocol = heldMeetingProtocol(title);
        Long actor = users.register("pub-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Editor").id();
        writer.attachFile(protocol.id(), "minutes.pdf", new ByteArrayInputStream("%PDF-1.4".getBytes()));
        writer.approve(protocol.id(), actor);
        writer.publish(protocol.id(), actor);

        String page = mvc.perform(get("/protocols/" + protocol.id())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains(title).contains("Опубліковано");
        mvc.perform(get("/protocols/" + protocol.id() + "/file")).andExpect(status().isOk());
        String meetingPage = mvc.perform(get("/meetings/" + protocol.meetingId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(meetingPage).contains("/protocols/" + protocol.id());
    }
}
