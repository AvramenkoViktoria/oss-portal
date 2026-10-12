package ua.edu.ukma.oss.protocol;

import java.time.Instant;

/**
 * Read model of a protocol.
 *
 * @param fileKey     storage key of the attached document, {@code null} while none is attached
 * @param fileName    original name of the attached document
 * @param approvedBy  user who approved, {@code null} before approval
 * @param publishedBy user who published, {@code null} before publication
 */
public record ProtocolDto(Long id, Long meetingId, Long orgId, ProtocolStatus status, String fileKey,
                          String fileName, Instant createdAt, Instant approvedAt, Long approvedBy,
                          Instant publishedAt, Long publishedBy) {

    public boolean hasFile() {
        return fileKey != null;
    }
}
