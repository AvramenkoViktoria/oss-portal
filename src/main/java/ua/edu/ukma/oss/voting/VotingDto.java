package ua.edu.ukma.oss.voting;

/**
 * Read model of a voting.
 *
 * @param agendaItemId agenda item the question belongs to, may be {@code null}
 * @param result       {@code null} until votes are recorded
 */
public record VotingDto(Long id, Long protocolId, Long agendaItemId, String question, VoteResult result,
                        int votesFor, int votesAgainst, int votesAbstain) {

    public boolean isRecorded() {
        return result != null;
    }
}
