package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class PetitionWebIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserService users;
    @Autowired
    AuthService auth;
    @Autowired
    OrganizationService organizations;

    private Long userId;
    private Long membershipId;

    /**
     * Web tests commit their data, so a membership must not stay active and distort other tests.
     */
    @AfterEach
    void endMembership() {
        if (membershipId != null) {
            organizations.endTerm(membershipId, LocalDate.now().minusDays(1));
        }
    }

    private Cookie signIn() {
        String email = "pet-" + UUID.randomUUID() + "@ukma.edu.ua";
        userId = users.register(email, "s3cret-pass", "Petitioner").id();
        return new Cookie("OSS_TOKEN", auth.login(email, "s3cret-pass"));
    }

    @Test
    void studentSubmitsPetitionAndSeesItInTheirList() throws Exception {
        Cookie token = signIn();
        Long sk = organizations.getByCode(OrgCode.SK).id();
        String title = "Petition-" + UUID.randomUUID();

        mvc.perform(post("/petitions").with(csrf()).cookie(token)
                        .param("orgId", sk.toString()).param("title", title).param("text", "Please consider"))
                .andExpect(redirectedUrl("/petitions/mine"));

        String page = mvc.perform(get("/petitions/mine").cookie(token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains(title).contains("Подано");
    }

    @Test
    void memberOfABodyGetsARuleMessageInsteadOfASubmission() throws Exception {
        Cookie token = signIn();
        Long ks = organizations.getByCode(OrgCode.KS).id();
        membershipId = organizations.addMember(
                new AddMemberCmd(userId, ks, "FI", "Delegate", LocalDate.now().minusDays(1), null)).id();

        mvc.perform(post("/petitions").with(csrf()).cookie(token).header("Referer", "http://localhost/petitions/new")
                        .param("orgId", ks.toString()).param("title", "T").param("text", "X"))
                .andExpect(redirectedUrl("http://localhost/petitions/new"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void anonymousVisitorIsSentToLogin() throws Exception {
        mvc.perform(get("/petitions/new")).andExpect(redirectedUrl("/login?next=%2Fpetitions%2Fnew"));
    }
}
