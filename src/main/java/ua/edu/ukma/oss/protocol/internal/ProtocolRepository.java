package ua.edu.ukma.oss.protocol.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.ukma.oss.protocol.ProtocolStatus;

interface ProtocolRepository extends JpaRepository<Protocol, Long> {

    Optional<Protocol> findByMeetingId(Long meetingId);

    List<Protocol> findByOrgIdAndStatusOrderByPublishedAtDesc(Long orgId, ProtocolStatus status);

    List<Protocol> findByOrgIdOrderByCreatedAtDesc(Long orgId);
}
