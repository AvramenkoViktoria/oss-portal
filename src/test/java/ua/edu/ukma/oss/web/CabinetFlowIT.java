package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.support.IntegrationTest;
import ua.edu.ukma.oss.voting.VotingService;

/**
 * Drives the editors' cabinet end to end through HTTP: meeting, protocol lifecycle, voting.
 */
@IntegrationTest
@AutoConfigureMockMvc
class CabinetFlowIT {

    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    MockMvc mvc;
    @Autowired
    UserService users;
    @Autowired
    AuthService auth;
    @Autowired
    OrganizationService organizations;
    @Autowired
    MeetingService meetings;
    @Autowired
    ProtocolReader protocols;
    @Autowired
    VotingService votings;

    Long sk;
    Long ks;
    Cookie editor;
    Cookie student;
    final List<Long> memberships = new ArrayList<>();

    @BeforeEach
    void setUp() {
        sk = organizations.getByCode(OrgCode.SK).id();
        ks = organizations.getByCode(OrgCode.KS).id();
        editor = signIn("Editor", Role.SK_MEMBER, sk);
        student = signIn("Student", null, null);
        // Two seated members to vote; ended again in tearDown so other tests see no leftovers.
        for (int i = 0; i < 2; i++) {
            Long userId = users.register("seat-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Member " + i).id();
            MembershipDto m = organizations.addMember(new AddMemberCmd(userId, sk, "FI", "Member",
                    LocalDate.now().minusDays(10), null));
            memberships.add(m.id());
        }
    }

    @AfterEach
    void tearDown() {
        memberships.forEach(id -> organizations.endTerm(id, LocalDate.now().minusDays(4)));
    }

    private Cookie signIn(String name, Role role, Long orgId) {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@ukma.edu.ua";
        Long id = users.register(email, "s3cret-pass", name).id();
        if (role != null) {
            users.grant(id, role, orgId);
        }
        return new Cookie("OSS_TOKEN", auth.login(email, "s3cret-pass"));
    }

    private static String past() {
        return LocalDateTime.ofInstant(Instant.now().minus(2, ChronoUnit.HOURS), ZoneId.systemDefault()).format(INPUT);
    }

    @Test
    void studentCannotEnterCabinetAndEditorCannotEnterAnotherBody() throws Exception {
        mvc.perform(get("/cabinet/sk").cookie(student)).andExpect(status().isForbidden());
        mvc.perform(get("/cabinet/ks").cookie(editor)).andExpect(status().isForbidden());
        mvc.perform(get("/cabinet/sk").cookie(editor)).andExpect(status().isOk());
    }

    @Test
    void editorCannotReachMeetingOfAnotherBodyByGuessingItsId() throws Exception {
        MeetingDto other = meetings.schedule(new ScheduleCmd(ks, "KS session", Instant.now().plus(1, ChronoUnit.DAYS), null));

        mvc.perform(get("/cabinet/sk/meetings/" + other.id()).cookie(editor)).andExpect(status().isNotFound());
        mvc.perform(post("/cabinet/sk/meetings/" + other.id() + "/cancel").with(csrf()).cookie(editor))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullProtocolLifecycleThroughTheCabinet() throws Exception {
        String title = "Journey-" + UUID.randomUUID();

        // 1. Schedule a meeting (already started) and give it an agenda.
        MvcResult created = mvc.perform(post("/cabinet/sk/meetings").with(csrf()).cookie(editor)
                        .param("title", title).param("scheduledAt", past()))
                .andExpect(redirectedUrlPattern("/cabinet/sk/meetings/*")).andReturn();
        String meetingUrl = created.getResponse().getRedirectedUrl();
        mvc.perform(post(meetingUrl + "/agenda").with(csrf()).cookie(editor).param("title", "Budget"))
                .andExpect(status().is3xxRedirection());

        // 2. Hold it: a draft protocol appears and the user lands on its workspace.
        MvcResult held = mvc.perform(post(meetingUrl + "/held").with(csrf()).cookie(editor))
                .andExpect(redirectedUrlPattern("/cabinet/sk/protocols/*")).andReturn();
        String protocolUrl = held.getResponse().getRedirectedUrl();
        Long protocolId = Long.valueOf(protocolUrl.substring(protocolUrl.lastIndexOf('/') + 1));
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.DRAFT);

        // 3. Approval is refused while the checklist is incomplete.
        mvc.perform(post(protocolUrl + "/approve").with(csrf()).cookie(editor).header("Referer", "http://localhost" + protocolUrl))
                .andExpect(flash().attributeExists("error"));
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.DRAFT);

        // 4. Attach the document and record a voting.
        mvc.perform(multipart(protocolUrl + "/file")
                        .file(new MockMultipartFile("file", "minutes.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .with(csrf()).cookie(editor))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(protocolUrl + "/votings").with(csrf()).cookie(editor).param("question", "Approve the budget?"))
                .andExpect(status().is3xxRedirection());
        String workspace = mvc.perform(get(protocolUrl).cookie(editor)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(workspace).contains("Approve the budget?").contains("Чернетка");
        Long votingId = firstVotingId(protocolId);
        var request = post(protocolUrl + "/votings/" + votingId + "/record").with(csrf()).cookie(editor);
        for (Long membershipId : memberships) {
            request.param("choice_" + membershipId, "FOR");
        }
        mvc.perform(request).andExpect(status().is3xxRedirection());

        // 5. Approve, then publish.
        mvc.perform(post(protocolUrl + "/approve").with(csrf()).cookie(editor)).andExpect(status().is3xxRedirection());
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.APPROVED);
        String locked = mvc.perform(get(protocolUrl).cookie(editor)).andReturn().getResponse().getContentAsString();
        assertThat(locked).contains("заблоковано");
        mvc.perform(post(protocolUrl + "/publish").with(csrf()).cookie(editor)).andExpect(status().is3xxRedirection());
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.PUBLISHED);

        // 6. The public sees the protocol and its voting.
        String publicPage = mvc.perform(get("/protocols/" + protocolId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(publicPage).contains(title).contains("Approve the budget?").contains("Рішення прийнято");
    }

    private Long firstVotingId(Long protocolId) {
        return votings.byProtocol(protocolId).getFirst().id();
    }
}
