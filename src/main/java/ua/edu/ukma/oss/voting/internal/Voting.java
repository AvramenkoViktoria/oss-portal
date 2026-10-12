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

    @Column(name = "agenda_item_id")
    private Long agendaItemId;

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

    protected Voting() {
    }

    Voting(Long protocolId, Long agendaItemId, String question) {
        this.protocolId = protocolId;
        this.agendaItemId = agendaItemId;
        this.question = question;
    }

    Long getProtocolId() {
        return protocolId;
    }

    Long getAgendaItemId() {
        return agendaItemId;
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

    void applyTally(VoteCounter.Counts counts) {
        this.votesFor = counts.votesFor();
        this.votesAgainst = counts.votesAgainst();
        this.votesAbstain = counts.votesAbstain();
        this.result = counts.result();
    }
}
