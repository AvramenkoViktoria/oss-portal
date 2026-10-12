package ua.edu.ukma.oss.meeting.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.shared.BaseEntity;

@Entity
@Table(name = "agenda_item")
class AgendaItem extends BaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    protected AgendaItem() {
    }

    AgendaItem(Long meetingId, int position, String title) {
        this.meetingId = meetingId;
        this.position = position;
        this.title = title;
    }

    Long getMeetingId() {
        return meetingId;
    }

    int getPosition() {
        return position;
    }

    String getTitle() {
        return title;
    }
}
