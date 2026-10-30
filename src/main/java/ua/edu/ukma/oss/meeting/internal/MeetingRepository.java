package ua.edu.ukma.oss.meeting.internal;

import java.time.Instant;
import java.util.List;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByOrgIdOrderByScheduledAtDesc(Long orgId);

    List<Meeting> findByStatusAndReminderSentAtIsNullAndScheduledAtAfterAndScheduledAtLessThanEqualOrderByScheduledAt(
            MeetingStatus status, Instant after, Instant until);
}
