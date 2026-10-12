package ua.edu.ukma.oss.organization.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.ukma.oss.organization.OrgCode;

interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByCode(OrgCode code);
}
