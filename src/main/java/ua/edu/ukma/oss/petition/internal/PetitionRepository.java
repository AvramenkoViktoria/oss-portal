package ua.edu.ukma.oss.petition.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.ukma.oss.petition.PetitionStatus;

interface PetitionRepository extends JpaRepository<Petition, Long> {

    List<Petition> findByOrgIdOrderByCreatedAtDesc(Long orgId);

    List<Petition> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Petition> findByMeetingIdOrderByCreatedAt(Long meetingId);

    List<Petition> findByMeetingIdAndStatus(Long meetingId, PetitionStatus status);
}
