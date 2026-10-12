package ua.edu.ukma.oss.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.support.IntegrationTest;

@IntegrationTest
@Transactional
class OrganizationServiceIT {

    @Autowired
    OrganizationService organizations;
    @Autowired
    UserService users;

    private Long newUser() {
        return users.register("member-" + UUID.randomUUID() + "@ukma.edu.ua", "s3cret-pass", "Member").id();
    }

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
        Long current = newUser();
        Long expired = newUser();
        organizations.addMember(new AddMemberCmd(current, sk, "FI", "Chair", today.minusMonths(1), null));
        organizations.addMember(new AddMemberCmd(expired, sk, "FI", "Member", today.minusYears(2), today.minusYears(1)));

        assertThat(organizations.members(sk)).extracting(MembershipDto::userId).contains(current).doesNotContain(expired);
        assertThat(organizations.history(sk)).extracting(MembershipDto::userId).contains(current, expired);
        assertThat(organizations.isMember(current)).isTrue();
        assertThat(organizations.isMember(expired)).isFalse();
    }

    @Test
    void endingATermDeactivatesMembership() {
        Long ks = organizations.getByCode(OrgCode.KS).id();
        LocalDate today = LocalDate.now();
        Long user = newUser();
        MembershipDto m = organizations.addMember(new AddMemberCmd(user, ks, "FEN", "Delegate", today.minusDays(5), null));

        organizations.endTerm(m.id(), today.minusDays(1));

        assertThat(organizations.isMember(user, ks)).isFalse();
    }

    @Test
    void overlappingTermIsRejected() {
        Long ks = organizations.getByCode(OrgCode.KS).id();
        LocalDate today = LocalDate.now();
        Long user = newUser();
        organizations.addMember(new AddMemberCmd(user, ks, "FEN", "Delegate", today.minusDays(5), null));

        assertThatThrownBy(() -> organizations.addMember(
                new AddMemberCmd(user, ks, "FEN", "Delegate", today, today.plusMonths(3))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void channelCanBeRepointed() {
        Long sk = organizations.getByCode(OrgCode.SK).id();

        organizations.updateChannel(sk, Messenger.TELEGRAM, "telegram:@sk");

        assertThat(organizations.get(sk).channelId()).isEqualTo("telegram:@sk");
    }
}
