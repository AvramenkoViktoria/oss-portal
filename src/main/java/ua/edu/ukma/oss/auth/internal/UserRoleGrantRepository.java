package ua.edu.ukma.oss.auth.internal;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRoleGrantRepository extends JpaRepository<UserRoleGrant, Long> {

    List<UserRoleGrant> findByUserId(Long userId);

    List<UserRoleGrant> findByUserIdIn(Collection<Long> userIds);
}
