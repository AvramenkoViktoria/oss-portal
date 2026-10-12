package ua.edu.ukma.oss.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Account and role administration.
 */
public interface UserService {

    /**
     * Creates a student account.
     *
     * @throws ua.edu.ukma.oss.shared.BusinessRuleException on duplicate email or weak password
     */
    UserDto register(String email, String password, String fullName);

    UserDto get(Long id);

    List<UserDto> all();

    /**
     * Display names for the given ids; unknown ids are absent from the result.
     */
    Map<Long, String> fullNames(Collection<Long> ids);

    UserDto grant(Long userId, Role role, Long orgId);

    UserDto revoke(Long grantId);

    UserDto setActive(Long userId, boolean active);
}
