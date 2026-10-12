package ua.edu.ukma.oss.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.auth.AuthenticationFailedException;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * Login, registration and logout. The token travels in an HttpOnly cookie.
 */
@Controller
class AuthController {

    private static final String COOKIE = "OSS_TOKEN";

    private final AuthService auth;
    private final UserService users;
    private final Duration tokenTtl;

    AuthController(AuthService auth, UserService users, @Value("${oss.security.token-ttl}") Duration tokenTtl) {
        this.auth = auth;
        this.users = users;
        this.tokenTtl = tokenTtl;
    }

    @GetMapping("/login")
    String loginForm(@RequestParam(required = false) String next, Model model) {
        model.addAttribute("next", safeNext(next));
        return "pages/login";
    }

    @PostMapping("/login")
    String login(@RequestParam String email, @RequestParam String password,
                 @RequestParam(required = false) String next, HttpServletRequest request,
                 HttpServletResponse response, RedirectAttributes redirect) {
        try {
            addTokenCookie(response, auth.login(email, password), request.isSecure());
            return "redirect:" + safeNext(next);
        } catch (AuthenticationFailedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    String registerForm() {
        return "pages/register";
    }

    @PostMapping("/register")
    String register(@RequestParam String fullName, @RequestParam String email, @RequestParam String password,
                    HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        try {
            users.register(email, password, fullName);
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
        addTokenCookie(response, auth.login(email, password), request.isSecure());
        return "redirect:/";
    }

    @PostMapping("/logout")
    String logout(HttpServletRequest request, HttpServletResponse response) {
        addCookie(response, "", Duration.ZERO, request.isSecure());
        return "redirect:/";
    }

    private void addTokenCookie(HttpServletResponse response, String token, boolean secure) {
        addCookie(response, token, tokenTtl, secure);
    }

    private static void addCookie(HttpServletResponse response, String value, Duration maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE, value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Only same-site absolute paths are honoured, so the login form cannot redirect elsewhere.
     */
    static String safeNext(String next) {
        if (next == null || !next.startsWith("/") || next.startsWith("//") || next.contains("\\")) {
            return "/";
        }
        return next;
    }
}
