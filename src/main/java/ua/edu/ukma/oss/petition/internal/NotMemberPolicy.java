package ua.edu.ukma.oss.petition.internal;

import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.petition.AuthorEligibilityPolicy;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * Petitions come from the student body at large: anyone currently seated in the SK or the KS
 * can act through the body itself and may not petition it.
 */
@Component
class NotMemberPolicy implements AuthorEligibilityPolicy {

    private final OrganizationService organizations;

    NotMemberPolicy(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @Override
    public void ensureEligible(Long userId, Long orgId) {
        if (organizations.isMember(userId)) {
            throw new BusinessRuleException("Члени СК та КС не можуть подавати петиції");
        }
    }
}
