package ua.edu.ukma.oss.organization;

import java.time.LocalDate;

/**
 * Read model of one person's membership in an organization for a term of office.
 *
 * @param termEnd {@code null} while the term is open-ended
 */
public record MembershipDto(Long id, Long userId, Long orgId, String faculty, String position,
                            LocalDate termStart, LocalDate termEnd) {

    /**
     * Whether the term covers the given day (both bounds inclusive).
     */
    public boolean isActiveOn(LocalDate day) {
        return !termStart.isAfter(day) && (termEnd == null || !termEnd.isBefore(day));
    }
}
