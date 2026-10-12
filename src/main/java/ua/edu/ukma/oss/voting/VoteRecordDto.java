package ua.edu.ukma.oss.voting;

/**
 * One member's vote.
 */
public record VoteRecordDto(Long membershipId, VoteChoice choice) {
}
