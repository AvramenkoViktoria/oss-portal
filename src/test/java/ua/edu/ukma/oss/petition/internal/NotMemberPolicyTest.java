package ua.edu.ukma.oss.petition.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class NotMemberPolicyTest {

    private final OrganizationService organizations = mock(OrganizationService.class);
    private final NotMemberPolicy policy = new NotMemberPolicy(organizations);

    @Test
    void rejectsCurrentMember() {
        when(organizations.isMember(5L)).thenReturn(true);

        assertThatThrownBy(() -> policy.ensureEligible(5L, 1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void acceptsOrdinaryStudent() {
        assertThatCode(() -> policy.ensureEligible(6L, 1L)).doesNotThrowAnyException();
    }
}
