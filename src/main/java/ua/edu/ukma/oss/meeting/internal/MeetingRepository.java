package ua.edu.ukma.oss.meeting.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOrgIdOrderByScheduledAtDesc(Long orgId);
}
