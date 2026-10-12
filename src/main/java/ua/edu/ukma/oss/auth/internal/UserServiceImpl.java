package ua.edu.ukma.oss.auth.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.RoleGrantDto;
import ua.edu.ukma.oss.auth.UserDto;
import ua.edu.ukma.oss.auth.UserService;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class UserServiceImpl implements UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final UserRoleGrantRepository grants;
    private final PasswordEncoder encoder;

    UserServiceImpl(UserRepository users, UserRoleGrantRepository grants, PasswordEncoder encoder) {
        this.users = users;
        this.grants = grants;
        this.encoder = encoder;
    }

    @Override
    public UserDto register(String email, String password, String fullName) {
        String normalized = JwtAuthService.normalize(email);
        if (normalized.isEmpty() || !normalized.contains("@")) {
            throw new BusinessRuleException("A valid email is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessRuleException("Full name is required");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (users.existsByEmail(normalized)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        User user = users.save(new User(normalized, encoder.encode(password), fullName.trim()));
        return toDto(user, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
        return toDto(user, grants.findByUserId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> all() {
        List<User> all = users.findAll();
        Map<Long, List<UserRoleGrant>> byUser = grants.findAll().stream()
                .collect(Collectors.groupingBy(UserRoleGrant::getUserId));
        return all.stream()
                .map(u -> toDto(u, byUser.getOrDefault(u.getId(), List.of())))
                .sorted((a, b) -> a.fullName().compareToIgnoreCase(b.fullName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> fullNames(Collection<Long> ids) {
        return users.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getFullName));
    }

    @Override
    public UserDto grant(Long userId, Role role, Long orgId) {
        if (role == Role.STUDENT) {
            throw new BusinessRuleException("The student role is implicit and cannot be granted");
        }
        if (role.editsOrganizationContent() && orgId == null) {
            throw new BusinessRuleException("This role must be granted for a specific organization");
        }
        if (role == Role.ADMIN && orgId != null) {
            throw new BusinessRuleException("The admin role is not tied to an organization");
        }
        users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
        boolean exists = grants.findByUserId(userId).stream()
                .anyMatch(g -> g.getRole() == role && java.util.Objects.equals(g.getOrgId(), orgId));
        if (!exists) {
            grants.save(new UserRoleGrant(userId, role, orgId));
        }
        return get(userId);
    }

    @Override
    public UserDto revoke(Long grantId) {
        UserRoleGrant grant = grants.findById(grantId).orElseThrow(() -> new NotFoundException("Role grant", grantId));
        grants.delete(grant);
        return get(grant.getUserId());
    }

    @Override
    public UserDto setActive(Long userId, boolean active) {
        users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId)).setActive(active);
        return get(userId);
    }

    private static UserDto toDto(User user, List<UserRoleGrant> userGrants) {
        List<RoleGrantDto> dtos = userGrants.stream()
                .map(g -> new RoleGrantDto(g.getId(), g.getRole(), g.getOrgId()))
                .toList();
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.isActive(), dtos);
    }
}
