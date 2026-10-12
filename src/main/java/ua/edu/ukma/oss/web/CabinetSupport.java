package ua.edu.ukma.oss.web;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;
import ua.edu.ukma.oss.web.support.Formats;

/**
 * Small helpers shared by the cabinet controllers.
 */
@Component
class CabinetSupport {

    private final MeetingService meetings;
    private final Formats formats;

    CabinetSupport(MeetingService meetings, java.time.Clock clock) {
        this.meetings = meetings;
        this.formats = new Formats(clock.getZone());
    }

    /**
     * Loads a meeting and makes sure it belongs to the body from the URL, so an editor of one
     * body cannot reach another body's data by changing an id.
     */
    MeetingDto meetingOf(OrganizationDto org, Long meetingId) {
        MeetingDto meeting = meetings.get(meetingId);
        if (!meeting.orgId().equals(org.id())) {
            throw new NotFoundException("Meeting", meetingId);
        }
        return meeting;
    }

    Instant parseWhen(String value) {
        try {
            return formats.parseInput(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessRuleException("Вкажіть коректні дату й час");
        }
    }

    static String codeOf(OrganizationDto org) {
        return org.code().name().toLowerCase();
    }
}
