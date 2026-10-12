package ua.edu.ukma.oss.voting;

public record CreateVotingCmd(Long protocolId, Long agendaItemId, String question) {
}
