package ua.edu.ukma.oss.auth.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.shared.BaseEntity;

/**
 * A role held by a user, optionally scoped to one organization.
 */
@Entity
@Table(name = "user_role")
class UserRoleGrant extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "org_id")
    private Long orgId;

    protected UserRoleGrant() {
    }

    UserRoleGrant(Long userId, Role role, Long orgId) {
        this.userId = userId;
        this.role = role;
        this.orgId = orgId;
    }

    Long getUserId() {
        return userId;
    }

    Role getRole() {
        return role;
    }

    Long getOrgId() {
        return orgId;
    }
}
