package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.protocol.ProtocolReader;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.support.IntegrationTest;
import ua.edu.ukma.oss.voting.VotingDto;
import ua.edu.ukma.oss.voting.VotingService;
import ua.edu.ukma.oss.support.Docx;

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

    @BeforeEach
    void setUp() {
        sk = organizations.getByCode(OrgCode.SK).id();
        ks = organizations.getByCode(OrgCode.KS).id();
        editor = signIn("Editor", Role.SK_MEMBER, sk);
        student = signIn("Student", null, null);
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
    void thereIsNoStandaloneProtocolsSectionInTheCabinet() throws Exception {
        mvc.perform(get("/cabinet/sk/protocols").cookie(editor)).andExpect(status().isNotFound());
        String page = mvc.perform(get("/cabinet/sk").cookie(editor)).andReturn().getResponse().getContentAsString();
        assertThat(page).doesNotContain("/cabinet/sk/protocols\"");
    }

    @Test
    void cabinetMeetingsAreShownAsAMonthCalendar() throws Exception {
        String title = "Calendar-" + UUID.randomUUID();
        meetings.schedule(new ScheduleCmd(sk, title, Instant.now().plus(1, ChronoUnit.HOURS), null));

        String current = mvc.perform(get("/cabinet/sk/meetings").cookie(editor)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(current).contains("cal-grid").contains("Пн");

        String far = mvc.perform(get("/cabinet/sk/meetings").param("month", "2031-02").cookie(editor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(far).contains("лютий 2031").doesNotContain(title);
        mvc.perform(get("/cabinet/sk/meetings").param("month", "garbage").cookie(editor)).andExpect(status().isOk());
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

        // 2. Hold it: a draft protocol appears and is opened from the meeting page.
        mvc.perform(post(meetingUrl + "/held").with(csrf()).cookie(editor))
                .andExpect(redirectedUrl(meetingUrl));
        Long meetingId = Long.valueOf(meetingUrl.substring(meetingUrl.lastIndexOf('/') + 1));
        Long protocolId = protocols.byMeeting(meetingId).orElseThrow().id();
        String meetingPage = mvc.perform(get(meetingUrl).cookie(editor)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(meetingPage).contains("/cabinet/sk/protocols/" + protocolId);
        String protocolUrl = "/cabinet/sk/protocols/" + protocolId;
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.DRAFT);

        // 3. Approval is refused while the checklist is incomplete.
        mvc.perform(post(protocolUrl + "/approve").with(csrf()).cookie(editor).header("Referer", "http://localhost" + protocolUrl))
                .andExpect(flash().attributeExists("error"));
        assertThat(protocols.get(protocolId).status()).isEqualTo(ProtocolStatus.DRAFT);

        // 4. A protocol without the voting section is refused; one with it creates the votings itself.
        mvc.perform(multipart(protocolUrl + "/file")
                        .file(new MockMultipartFile("file", "minutes.docx", "application/octet-stream",
                                Docx.of("ПРОТОКОЛ", "Слухали: звіт.")))
                        .with(csrf()).cookie(editor).header("Referer", "http://localhost" + protocolUrl))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("розділу з результатами голосування")));
        assertThat(protocols.get(protocolId).hasFile()).isFalse();
        assertThat(votings.byProtocol(protocolId)).isEmpty();

        mvc.perform(multipart(protocolUrl + "/file")
                        .file(new MockMultipartFile("file", "minutes.docx", "application/octet-stream",
                                Docx.of("Поставили на голосування:", "Approve the budget?", "Результати голосування:",
                                        "За - 4", "Проти - 1", "Утримався - 0", "Не голосували - 1", "Рішення прийнято")))
                        .with(csrf()).cookie(editor))
                .andExpect(status().is3xxRedirection());
        String workspace = mvc.perform(get(protocolUrl).cookie(editor)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(workspace).contains("Approve the budget?").contains("Чернетка");
        assertThat(votings.byProtocol(protocolId)).singleElement()
                .extracting(VotingDto::votesFor, VotingDto::votesAgainst, VotingDto::votesAbsent, VotingDto::present)
                .containsExactly(4, 1, 1, 6);
        mvc.perform(post(protocolUrl + "/votings").with(csrf()).cookie(editor).param("question", "By hand"))
                .andExpect(status().is4xxClientError());

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
}
