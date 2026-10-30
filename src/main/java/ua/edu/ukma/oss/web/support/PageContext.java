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
                          boolean sealedMember, Formats fmt) {

    public boolean isAuthenticated() {
        return user != null;
    }

    /**
     * Whether the page may offer petitions: members of the SK and the KS act through their body instead.
     */
    public boolean canPetition() {
        return !sealedMember;
    }

    public boolean isAdmin() {
        return user != null && user.isAdmin();
    }
}
