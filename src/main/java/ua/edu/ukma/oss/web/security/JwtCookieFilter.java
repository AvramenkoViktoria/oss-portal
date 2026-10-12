package ua.edu.ukma.oss.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.AuthService;

/**
 * Authenticates a request from the token cookie. A missing or invalid token leaves the
 * request anonymous; the security rules decide what anonymous users may reach.
 */
@Component
class JwtCookieFilter extends OncePerRequestFilter {

    static final String COOKIE = "OSS_TOKEN";

    private final AuthService auth;

    JwtCookieFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = tokenFrom(request);
        if (token != null) {
            auth.authenticate(token).ifPresent(this::establish);
        }
        chain.doFilter(request, response);
    }

    private void establish(AuthPrincipal principal) {
        List<GrantedAuthority> authorities = principal.roles().stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
    }

    private static String tokenFrom(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
