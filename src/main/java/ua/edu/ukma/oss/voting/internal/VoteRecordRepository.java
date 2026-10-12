package ua.edu.ukma.oss.voting.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {

    List<VoteRecord> findByVotingIdOrderByMembershipId(Long votingId);

    void deleteByVotingId(Long votingId);
}
