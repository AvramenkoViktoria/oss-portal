package ua.edu.ukma.oss.voting.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface VotingRepository extends JpaRepository<Voting, Long> {

    List<Voting> findByProtocolIdOrderById(Long protocolId);
}
