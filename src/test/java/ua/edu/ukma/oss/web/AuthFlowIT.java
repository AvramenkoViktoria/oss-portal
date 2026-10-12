package ua.edu.ukma.oss.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class AuthFlowIT {

    @Autowired
    MockMvc mvc;

    @Test
    void homePageIsPublic() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void cabinetRedirectsAnonymousToLogin() throws Exception {
        mvc.perform(get("/cabinet/sk")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?next=%2Fcabinet%2Fsk"));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/login").param("email", "a@b.c").param("password", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerSetsHttpOnlyCookieAndSignsTheUserIn() throws Exception {
        String email = "web-" + UUID.randomUUID() + "@ukma.edu.ua";
        Cookie token = mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Web User").param("email", email).param("password", "s3cret-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().httpOnly("OSS_TOKEN", true))
                .andReturn().getResponse().getCookie("OSS_TOKEN");

        assertThat(token).isNotNull();
        String page = mvc.perform(get("/").cookie(token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("Web User").contains("Вийти");
    }

    @Test
    void wrongPasswordReturnsToLoginWithoutCookie() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("email", "nobody@ukma.edu.ua").param("password", "bad"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().doesNotExist("OSS_TOKEN"));
    }

    @Test
    void loginRedirectOnlyToLocalPaths() {
        assertThat(AuthController.safeNext("//evil.example")).isEqualTo("/");
        assertThat(AuthController.safeNext("https://evil.example")).isEqualTo("/");
        assertThat(AuthController.safeNext("/cabinet/sk")).isEqualTo("/cabinet/sk");
        assertThat(AuthController.safeNext(null)).isEqualTo("/");
    }
}
