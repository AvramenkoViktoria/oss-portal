package ua.edu.ukma.oss.meeting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class MeetingTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private Meeting meetingAt(Instant at) {
        return new Meeting(1L, "Regular session", at, null);
    }

    @Test
    void startsPlanned() {
        assertThat(meetingAt(NOW).getStatus()).isEqualTo(MeetingStatus.PLANNED);
    }

    @Test
    void canBeHeldOnceItHasStarted() {
        Meeting meeting = meetingAt(NOW.minusSeconds(60));

        meeting.markHeld(NOW);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.HELD);
    }

    @Test
    void cannotBeHeldBeforeItStarts() {
        Meeting meeting = meetingAt(NOW.plusSeconds(60));

        assertThatThrownBy(() -> meeting.markHeld(NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void heldMeetingIsFinal() {
        Meeting meeting = meetingAt(NOW.minusSeconds(60));
        meeting.markHeld(NOW);

        assertThatThrownBy(meeting::cancel).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> meeting.reschedule("x", NOW, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancelledMeetingCannotBeHeld() {
        Meeting meeting = meetingAt(NOW.minusSeconds(60));
        meeting.cancel();

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
        assertThatThrownBy(() -> meeting.markHeld(NOW)).isInstanceOf(BusinessRuleException.class);
    }
}
