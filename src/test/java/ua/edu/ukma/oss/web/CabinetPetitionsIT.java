package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.meeting.ScheduleCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.PetitionDto;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionService;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.petition.SubmitCmd;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class CabinetPetitionsIT {

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
    PetitionService petitions;

    Long sk;
    Cookie editor;
    Long authorId;

    @BeforeEach
    void setUp() {
        sk = organizations.getByCode(OrgCode.SK).id();
        String email = "ed-" + UUID.randomUUID() + "@ukma.edu.ua";
        Long editorId = users.register(email, "s3cret-pass", "Editor").id();
        users.grant(editorId, Role.SK_MEMBER, sk);
        editor = new Cookie("OSS_TOKEN", auth.login(email, "s3cret-pass"));
        authorId = users.register("au-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Author").id();
    }

    private PetitionDto petition() {
        return petitions.submit(new SubmitCmd(authorId, sk, "Petition " + UUID.randomUUID(), "Text"));
    }

    @Test
    void queueShowsPetitionWithItsAuthor() throws Exception {
        PetitionDto p = petition();

        String page = mvc.perform(get("/cabinet/sk/petitions").cookie(editor)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains(p.title()).contains("Author");
    }

    @Test
    void editorAddsPetitionToAgendaOfPlannedMeeting() throws Exception {
        PetitionDto p = petition();
        MeetingDto meeting = meetings.schedule(new ScheduleCmd(sk, "Session", Instant.now().plus(1, ChronoUnit.DAYS), null));

        mvc.perform(post("/cabinet/sk/petitions/" + p.id() + "/agenda").with(csrf()).cookie(editor)
                        .param("meetingId", meeting.id().toString()))
                .andExpect(status().is3xxRedirection());

        PetitionDto after = petitions.get(p.id());
        assertThat(after.status()).isEqualTo(PetitionStatus.SCHEDULED);
        assertThat(after.link()).isEqualTo(PetitionLink.AGENDA_ITEM);
        assertThat(meetings.get(meeting.id()).agenda()).hasSize(1);
    }

    @Test
    void editorSchedulesSeparateMeetingForPetition() throws Exception {
        PetitionDto p = petition();

        mvc.perform(post("/cabinet/sk/petitions/" + p.id() + "/separate").with(csrf()).cookie(editor)
                        .param("title", "Dedicated session").param("scheduledAt", "2099-01-01T10:00"))
                .andExpect(status().is3xxRedirection());

        assertThat(petitions.get(p.id()).link()).isEqualTo(PetitionLink.SEPARATE_MEETING);
    }

    @Test
    void editorRejectsPetitionWithReason() throws Exception {
        PetitionDto p = petition();

        mvc.perform(post("/cabinet/sk/petitions/" + p.id() + "/reject").with(csrf()).cookie(editor)
                        .param("reason", "Out of scope"))
                .andExpect(status().is3xxRedirection());

        assertThat(petitions.get(p.id()).status()).isEqualTo(PetitionStatus.REJECTED);
    }

    @Test
    void editorOfAnotherBodyCannotHandleThePetition() throws Exception {
        PetitionDto p = petition();
        String email = "ks-" + UUID.randomUUID() + "@ukma.edu.ua";
        Long ksEditor = users.register(email, "s3cret-pass", "KS editor").id();
        users.grant(ksEditor, Role.KS_DELEGATE, organizations.getByCode(OrgCode.KS).id());
        Cookie ks = new Cookie("OSS_TOKEN", auth.login(email, "s3cret-pass"));

        mvc.perform(post("/cabinet/ks/petitions/" + p.id() + "/reject").with(csrf()).cookie(ks).param("reason", "x"))
                .andExpect(status().isNotFound());
        assertThat(petitions.get(p.id()).status()).isEqualTo(PetitionStatus.SUBMITTED);
    }
}
