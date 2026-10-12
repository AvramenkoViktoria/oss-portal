package ua.edu.ukma.oss.meeting.internal;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AgendaItemRepository extends JpaRepository<AgendaItem, Long> {

    List<AgendaItem> findByMeetingIdOrderByPosition(Long meetingId);

    List<AgendaItem> findByMeetingIdInOrderByPosition(Collection<Long> meetingIds);

    int countByMeetingId(Long meetingId);
}
