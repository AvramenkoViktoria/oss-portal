package ua.edu.ukma.oss.auth.internal;

import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.auth.AuthPrincipal;
import ua.edu.ukma.oss.auth.OrgAccessPolicy;
import ua.edu.ukma.oss.auth.OrgAction;

/**
 * Members of a body edit its content; composition is managed by administrators only.
 * Administrators deliberately cannot edit a body's protocols, so published records stay
 * under the control of the body that owns them.
 */
@Component
class RoleBasedOrgAccessPolicy implements OrgAccessPolicy {

    @Override
    public boolean allows(AuthPrincipal principal, Long orgId, OrgAction action) {
        if (principal == null) {
            return false;
        }
        return switch (action) {
            case EDIT_CONTENT -> orgId != null && principal.orgIds().contains(orgId);
            case MANAGE_COMPOSITION -> principal.isAdmin();
        };
    }
}
