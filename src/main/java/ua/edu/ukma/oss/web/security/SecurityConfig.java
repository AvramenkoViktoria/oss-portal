package ua.edu.ukma.oss.web.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Stateless security: identity comes from the token cookie on every request. Reading the
 * site is open; petitions, the cabinets and administration require a signed-in user.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtCookieFilter jwtFilter) throws Exception {
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        // The token cookie authenticates every request anew; the default strategy would
                        // rotate the CSRF token each time and invalidate forms open in other tabs.
                        .sessionAuthenticationStrategy((authentication, request, response) -> { })
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/cabinet/**", "/petitions/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    // Remember where a signed-out user was heading so login can bring them back.
                    String target = "GET".equals(request.getMethod())
                            ? "/login?next=" + URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8)
                            : "/login";
                    response.sendRedirect(target);
                }))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Credentials are checked by the auth module, so Spring's default in-memory user must not exist.
     */
    @Bean
    UserDetailsService noUserDetails() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
