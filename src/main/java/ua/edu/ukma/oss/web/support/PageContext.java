package ua.edu.ukma.oss.web.support;

import java.util.List;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.organization.OrganizationDto;

/**
 * Data every page needs: who is looking, the CSRF token for forms, flash messages and the
 * bodies the user may edit (for the navigation).
 */
public record PageContext(AuthPrincipal user, String csrfParam, String csrfToken, String path,
                          String flashSuccess, String flashError, List<OrganizationDto> editableOrgs,
                          Formats fmt) {

    public boolean isAuthenticated() {
        return user != null;
    }

    public boolean isAdmin() {
        return user != null && user.isAdmin();
    }
}
