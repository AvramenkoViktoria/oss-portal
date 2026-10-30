package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.notification.NotificationAdminService;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class AdminIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserService users;
    @Autowired
    AuthService auth;
    @Autowired
    OrganizationService organizations;
    @Autowired
    NotificationAdminService notifications;

    Cookie admin;
    Long adminId;
    Cookie student;
    Long ks;

    @BeforeEach
    void setUp() {
        ks = organizations.getByCode(OrgCode.KS).id();
        String adminEmail = "admin-" + UUID.randomUUID() + "@ukma.edu.ua";
        adminId = users.register(adminEmail, "s3cret-pass", "Admin").id();
        users.grant(adminId, Role.ADMIN, null);
        admin = new Cookie("OSS_TOKEN", auth.login(adminEmail, "s3cret-pass"));
        String studentEmail = "st-" + UUID.randomUUID() + "@ukma.edu.ua";
        users.register(studentEmail, "s3cret-pass", "Student");
        student = new Cookie("OSS_TOKEN", auth.login(studentEmail, "s3cret-pass"));
    }

    @AfterEach
    void cleanUp() {
        notifications.simulateOutage(Messenger.TELEGRAM, false);
        // Terms started 10 days ago and are closed in the past so they do not leak into other tests.
        organizations.history(ks).stream().filter(m -> m.isActiveOn(LocalDate.now()))
                .filter(m -> m.termStart().isBefore(LocalDate.now().minusDays(1)))
                .forEach(m -> organizations.endTerm(m.id(), m.termStart()));
    }

    @Test
    void onlyAdministratorsReachAdministration() throws Exception {
        mvc.perform(get("/admin").cookie(student)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/notifications").cookie(student)).andExpect(status().isForbidden());
        mvc.perform(get("/admin").cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/orgs/ks").cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/notifications").cookie(admin)).andExpect(status().isOk());
    }

    @Test
    void addingMemberCanGrantEditorRoleAndEndingTheTermRevokesIt() throws Exception {
        String email = "delegate-" + UUID.randomUUID() + "@ukma.edu.ua";
        Long userId = users.register(email, "s3cret-pass", "Delegate").id();
        LocalDate start = LocalDate.now().minusDays(10);

        mvc.perform(post("/admin/orgs/ks/members").with(csrf()).cookie(admin)
                        .param("userId", userId.toString()).param("faculty", "FI").param("position", "Delegate")
                        .param("termStart", start.toString()).param("grantEditor", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(organizations.isMember(userId, ks)).isTrue();
        assertThat(auth.canEdit(auth.authenticate(auth.login(email, "s3cret-pass")).orElseThrow(), ks)).isTrue();

        MembershipDto membership = organizations.history(ks).stream()
                .filter(m -> m.userId().equals(userId)).findFirst().orElseThrow();
        mvc.perform(post("/admin/orgs/ks/members/" + membership.id() + "/end").with(csrf()).cookie(admin)
                        .param("termEnd", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(auth.canEdit(auth.authenticate(auth.login(email, "s3cret-pass")).orElseThrow(), ks)).isFalse();
    }

    @Test
    void administratorCanToggleMessengerOutage() throws Exception {
        mvc.perform(post("/admin/notifications/outage").with(csrf()).cookie(admin)
                        .param("messenger", "TELEGRAM").param("outage", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(notifications.isOutageSimulated(Messenger.TELEGRAM)).isTrue();
    }

    @Test
    void administratorMakesAndUnmakesAnotherAdministrator() throws Exception {
        Long id = users.register("r-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Roleless").id();

        mvc.perform(post("/admin/users/" + id + "/admin").with(csrf()).cookie(admin).param("admin", "true"))
                .andExpect(status().is3xxRedirection());
        assertThat(users.get(id).grants()).extracting(g -> g.role()).containsExactly(Role.ADMIN);

        mvc.perform(post("/admin/users/" + id + "/admin").with(csrf()).cookie(admin).param("admin", "false"))
                .andExpect(status().is3xxRedirection());
        assertThat(users.get(id).grants()).isEmpty();
    }

    @Test
    void editorRolesCannotBeGrantedOrRevokedOutsideTheCompositionPages() throws Exception {
        Long id = users.register("e-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Member").id();

        mvc.perform(post("/admin/users/" + id + "/grant").with(csrf()).cookie(admin)
                        .param("role", "KS_DELEGATE").param("orgId", ks.toString()))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/admin/grants/1/revoke").with(csrf()).cookie(admin)).andExpect(status().is4xxClientError());
        assertThat(users.get(id).grants()).isEmpty();
    }

    @Test
    void administratorCannotRemoveOwnAdministratorRole() throws Exception {
        mvc.perform(post("/admin/users/" + adminId + "/admin").with(csrf()).cookie(admin).param("admin", "false")
                        .header("Referer", "http://localhost/admin"))
                .andExpect(flash().attributeExists("error"));

        assertThat(users.get(adminId).grants()).hasSize(1);
    }

    @Test
    void disableButtonSendsAnExplicitValueAndTogglesTheAccount() throws Exception {
        Long id = users.register("d-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Toggled").id();

        String page = mvc.perform(get("/admin").cookie(admin)).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("name=\"active\" value=\"false\"");

        mvc.perform(post("/admin/users/" + id + "/active").with(csrf()).cookie(admin).param("active", "false"))
                .andExpect(status().is3xxRedirection());
        assertThat(users.get(id).active()).isFalse();
    }
}
