package ua.edu.ukma.oss.web;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.AuthService;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.NotFoundException;

/**
 * Resolves the body named in a URL and enforces editing rights on it.
 */
@Component
class WebAccess {

    private final OrganizationService organizations;
    private final AuthService auth;

    WebAccess(OrganizationService organizations, AuthService auth) {
        this.organizations = organizations;
        this.auth = auth;
    }

    /**
     * @param code lower- or upper-case body code from a path, such as {@code sk}
     */
    OrganizationDto org(String code) {
        try {
            return organizations.getByCode(OrgCode.valueOf(code.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Organization", code);
        }
    }

    OrganizationDto requireEditor(AuthPrincipal principal, String code) {
        OrganizationDto org = org(code);
        if (!auth.canEdit(principal, org.id())) {
            throw new AccessDeniedException("Not an editor of " + org.code());
        }
        return org;
    }

    boolean canEdit(AuthPrincipal principal, Long orgId) {
        return principal != null && auth.canEdit(principal, orgId);
    }
}
