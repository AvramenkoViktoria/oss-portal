package ua.edu.ukma.oss.notification.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxRepository extends JpaRepository<OutboxEntry, Long> {

    @Query("select e.id from OutboxEntry e where e.status = 'PENDING' and e.nextAttemptAt <= :now order by e.id")
    List<Long> findDueIds(@Param("now") Instant now);

    List<OutboxEntry> findAllByOrderByIdDesc(Pageable page);
}
