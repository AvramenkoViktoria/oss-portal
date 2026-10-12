package ua.edu.ukma.oss.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class OrganizationServiceIT {

    @Autowired
    OrganizationService organizations;

    @Test
    void bothBodiesAreSeededWithTheirMessengers() {
        OrganizationDto sk = organizations.getByCode(OrgCode.SK);
        OrganizationDto ks = organizations.getByCode(OrgCode.KS);

        assertThat(sk.messenger()).isEqualTo(Messenger.WHATSAPP);
        assertThat(ks.messenger()).isEqualTo(Messenger.TELEGRAM);
    }

    @Test
    void currentMemberIsVisibleAndExpiredOneIsNot() {
        Long sk = organizations.getByCode(OrgCode.SK).id();
        LocalDate today = LocalDate.now();
        organizations.addMember(new AddMemberCmd(101L, sk, "FI", "Chair", today.minusMonths(1), null));
        organizations.addMember(new AddMemberCmd(102L, sk, "FI", "Member", today.minusYears(2), today.minusYears(1)));

        assertThat(organizations.members(sk)).extracting(MembershipDto::userId).containsExactly(101L);
        assertThat(organizations.history(sk)).extracting(MembershipDto::userId).contains(101L, 102L);
        assertThat(organizations.isMember(101L)).isTrue();
        assertThat(organizations.isMember(102L)).isFalse();
    }

    @Test
    void endingATermDeactivatesMembership() {
        Long ks = organizations.getByCode(OrgCode.KS).id();
        LocalDate today = LocalDate.now();
        MembershipDto m = organizations.addMember(new AddMemberCmd(103L, ks, "FEN", "Delegate", today.minusDays(5), null));

        organizations.endTerm(m.id(), today.minusDays(1));

        assertThat(organizations.isMember(103L, ks)).isFalse();
    }

    @Test
    void overlappingTermIsRejected() {
        Long ks = organizations.getByCode(OrgCode.KS).id();
        LocalDate today = LocalDate.now();
        organizations.addMember(new AddMemberCmd(104L, ks, "FEN", "Delegate", today.minusDays(5), null));

        assertThatThrownBy(() -> organizations.addMember(
                new AddMemberCmd(104L, ks, "FEN", "Delegate", today, today.plusMonths(3))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void channelCanBeRepointed() {
        Long sk = organizations.getByCode(OrgCode.SK).id();

        organizations.updateChannel(sk, Messenger.TELEGRAM, "telegram:@sk");

        assertThat(organizations.get(sk).channelId()).isEqualTo("telegram:@sk");
    }
}
