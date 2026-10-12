package ua.edu.ukma.oss.organization;

import java.time.LocalDate;

/**
 * Command to register a person in an organization.
 */
public record AddMemberCmd(Long userId, Long orgId, String faculty, String position,
                           LocalDate termStart, LocalDate termEnd) {
}
