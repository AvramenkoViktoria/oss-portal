package ua.edu.ukma.oss.web.support;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;

/**
 * Supplies the {@link PageContext} to every view.
 */
@ControllerAdvice
class PageContextAdvice {

    private final OrganizationService organizations;
    private final Formats formats;

    PageContextAdvice(OrganizationService organizations, Clock clock) {
        this.organizations = organizations;
        this.formats = new Formats(clock.getZone());
    }

    @ModelAttribute("ctx")
    PageContext pageContext(@AuthenticationPrincipal AuthPrincipal user, HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute("_csrf");
        Map<String, ?> flash = RequestContextUtils.getInputFlashMap(request);
        List<OrganizationDto> editable = user == null ? List.of() : organizations.all().stream()
                .filter(o -> user.orgIds().contains(o.id())).toList();
        return new PageContext(user,
                csrf == null ? "_csrf" : csrf.getParameterName(),
                csrf == null ? "" : csrf.getToken(),
                request.getRequestURI(),
                flash == null ? null : (String) flash.get("success"),
                flash == null ? null : (String) flash.get("error"),
                editable, formats);
    }
}
