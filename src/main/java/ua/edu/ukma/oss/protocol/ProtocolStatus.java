package ua.edu.ukma.oss.protocol;

/**
 * Lifecycle of a protocol. Transitions are strictly {@code DRAFT -> APPROVED -> PUBLISHED}.
 */
public enum ProtocolStatus {
    DRAFT,
    APPROVED,
    PUBLISHED
}
