package ua.edu.ukma.oss.petition;

import java.time.Instant;

/**
 * Read model of a petition.
 *
 * @param link            {@code null} until scheduled
 * @param meetingId       {@code null} until scheduled
 * @param rejectionReason set only for rejected petitions
 */
public record PetitionDto(Long id, String title, String text, Long authorId, Long orgId, PetitionStatus status,
                          PetitionLink link, Long meetingId, String rejectionReason, Instant createdAt) {
}
