package ua.edu.ukma.oss.voting.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.shared.BaseEntity;
import ua.edu.ukma.oss.voting.VoteResult;

@Entity
@Table(name = "voting")
class Voting extends BaseEntity {

    @Column(name = "protocol_id", nullable = false)
    private Long protocolId;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Enumerated(EnumType.STRING)
    private VoteResult result;

    @Column(name = "votes_for", nullable = false)
    private int votesFor;

    @Column(name = "votes_against", nullable = false)
    private int votesAgainst;

    @Column(name = "votes_abstain", nullable = false)
    private int votesAbstain;

    @Column(name = "votes_absent", nullable = false)
    private int votesAbsent;

    @Column(nullable = false)
    private int present;

    protected Voting() {
    }

    Voting(Long protocolId, ParsedVoting parsed) {
        this.protocolId = protocolId;
        this.question = parsed.question();
        this.result = parsed.result();
        this.votesFor = parsed.votesFor();
        this.votesAgainst = parsed.votesAgainst();
        this.votesAbstain = parsed.votesAbstain();
        this.votesAbsent = parsed.votesAbsent();
        this.present = parsed.present();
    }

    Long getProtocolId() {
        return protocolId;
    }

    String getQuestion() {
        return question;
    }

    VoteResult getResult() {
        return result;
    }

    int getVotesFor() {
        return votesFor;
    }

    int getVotesAgainst() {
        return votesAgainst;
    }

    int getVotesAbstain() {
        return votesAbstain;
    }

    int getVotesAbsent() {
        return votesAbsent;
    }

    int getPresent() {
        return present;
    }
}
