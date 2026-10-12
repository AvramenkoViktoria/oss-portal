package ua.edu.ukma.oss.organization;

import java.time.LocalDate;
import java.util.List;

/**
 * Public API of the organization registry.
 */
public interface OrganizationService {

    OrganizationDto get(Long id);

    OrganizationDto getByCode(OrgCode code);

    List<OrganizationDto> all();

    /**
     * Whether the user currently holds a membership in any organization.
     */
    boolean isMember(Long userId);

    boolean isMember(Long userId, Long orgId);

    /**
     * Memberships that are in force today.
     */
    List<MembershipDto> members(Long orgId);

    /**
     * Every membership of the organization, including past and future terms, newest term first.
     */
    List<MembershipDto> history(Long orgId);

    MembershipDto membership(Long membershipId);

    MembershipDto addMember(AddMemberCmd cmd);

    /**
     * Closes a membership at the given day (inclusive).
     */
    MembershipDto endTerm(Long membershipId, LocalDate termEnd);

    void updateChannel(Long orgId, Messenger messenger, String channelId);
}
