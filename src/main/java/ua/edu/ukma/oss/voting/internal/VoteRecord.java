package ua.edu.ukma.oss.voting.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.shared.BaseEntity;
import ua.edu.ukma.oss.voting.VoteChoice;

@Entity
@Table(name = "vote_record")
class VoteRecord extends BaseEntity {

    @Column(name = "voting_id", nullable = false)
    private Long votingId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice choice;

    protected VoteRecord() {
    }

    VoteRecord(Long votingId, Long membershipId, VoteChoice choice) {
        this.votingId = votingId;
        this.membershipId = membershipId;
        this.choice = choice;
    }

    Long getVotingId() {
        return votingId;
    }

    Long getMembershipId() {
        return membershipId;
    }

    VoteChoice getChoice() {
        return choice;
    }

    void setChoice(VoteChoice choice) {
        this.choice = choice;
    }
}
