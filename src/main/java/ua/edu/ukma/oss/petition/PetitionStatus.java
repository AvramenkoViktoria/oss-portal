package ua.edu.ukma.oss.petition;

/**
 * Lifecycle: {@code SUBMITTED} then {@code SCHEDULED} (bound to a meeting) then {@code CONSIDERED};
 * {@code REJECTED} is reachable from {@code SUBMITTED} only. A cancelled meeting sends
 * {@code SCHEDULED} petitions back to {@code SUBMITTED}.
 */
public enum PetitionStatus {
    SUBMITTED,
    SCHEDULED,
    CONSIDERED,
    REJECTED
}
