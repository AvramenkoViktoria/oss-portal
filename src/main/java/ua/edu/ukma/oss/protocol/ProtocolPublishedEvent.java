package ua.edu.ukma.oss.protocol;

/**
 * Published when a protocol becomes public. Listeners run in the publisher's transaction.
 */
public record ProtocolPublishedEvent(Long protocolId, Long orgId, Long meetingId) {
}
