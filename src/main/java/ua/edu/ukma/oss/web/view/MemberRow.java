package ua.edu.ukma.oss.web.view;

import java.time.LocalDate;

public record MemberRow(Long membershipId, String name, String faculty, String position, LocalDate termStart,
                        LocalDate termEnd) {
}
