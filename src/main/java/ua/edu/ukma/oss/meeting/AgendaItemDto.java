package ua.edu.ukma.oss.meeting;

public record AgendaItemDto(Long id, Long meetingId, int position, String title) {
}
