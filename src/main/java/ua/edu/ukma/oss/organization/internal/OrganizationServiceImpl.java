package ua.edu.ukma.oss.organization.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.organization.AddMemberCmd;
import ua.edu.ukma.oss.organization.MembershipDto;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class OrganizationServiceImpl implements OrganizationService {

    /** Upper bound used when a term has no end, so interval checks need no null handling. */
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    private final OrganizationRepository orgs;
    private final MembershipRepository members;
    private final Clock clock;

    OrganizationServiceImpl(OrganizationRepository orgs, MembershipRepository members, Clock clock) {
        this.orgs = orgs;
        this.members = members;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDto get(Long id) {
        return toDto(orgs.findById(id).orElseThrow(() -> new NotFoundException("Organization", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDto getByCode(OrgCode code) {
        return toDto(orgs.findByCode(code).orElseThrow(() -> new NotFoundException("Organization", code)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDto> all() {
        return orgs.findAll().stream().map(OrganizationServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long userId) {
        return members.existsActive(userId, today());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(Long userId, Long orgId) {
        return members.existsActiveInOrg(userId, orgId, today());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipDto> members(Long orgId) {
        return members.findActive(orgId, today()).stream().map(OrganizationServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipDto> history(Long orgId) {
        return members.findByOrgIdOrderByTermStartDescIdDesc(orgId).stream()
                .map(OrganizationServiceImpl::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipDto membership(Long membershipId) {
        return toDto(find(membershipId));
    }

    @Override
    public MembershipDto addMember(AddMemberCmd cmd) {
        if (cmd.termEnd() != null && cmd.termEnd().isBefore(cmd.termStart())) {
            throw new BusinessRuleException("Кінець каденції не може бути раніше за її початок");
        }
        orgs.findById(cmd.orgId()).orElseThrow(() -> new NotFoundException("Organization", cmd.orgId()));
        LocalDate end = cmd.termEnd() == null ? FAR_FUTURE : cmd.termEnd();
        if (members.existsOverlapping(cmd.userId(), cmd.orgId(), cmd.termStart(), end)) {
            throw new BusinessRuleException("У цього члена вже є каденція в цьому органі, що перетинається з указаним періодом");
        }
        return toDto(members.save(new Membership(cmd.userId(), cmd.orgId(), cmd.faculty(), cmd.position(),
                cmd.termStart(), cmd.termEnd())));
    }

    @Override
    public MembershipDto endTerm(Long membershipId, LocalDate termEnd) {
        Membership membership = find(membershipId);
        if (termEnd.isBefore(membership.getTermStart())) {
            throw new BusinessRuleException("Кінець каденції не може бути раніше за її початок");
        }
        membership.endOn(termEnd);
        return toDto(membership);
    }

    @Override
    public void updateChannel(Long orgId, Messenger messenger, String channelId) {
        orgs.findById(orgId).orElseThrow(() -> new NotFoundException("Organization", orgId))
                .redirectTo(messenger, channelId);
    }

    private Membership find(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Membership", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static OrganizationDto toDto(Organization o) {
        return new OrganizationDto(o.getId(), o.getCode(), o.getName(), o.getMessenger(), o.getChannelId());
    }

    private static MembershipDto toDto(Membership m) {
        return new MembershipDto(m.getId(), m.getUserId(), m.getOrgId(), m.getFaculty(), m.getPosition(),
                m.getTermStart(), m.getTermEnd());
    }
}
