package ua.edu.ukma.oss.meeting.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.shared.BaseEntity;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * A session of a student body. Status changes go through the methods below so illegal
 * transitions cannot be written directly.
 */
@Entity
@Table(name = "meeting")
class Meeting extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "stream_url")
    private String streamUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.PLANNED;

    protected Meeting() {
    }

    Meeting(Long orgId, String title, Instant scheduledAt, String streamUrl) {
        this.orgId = orgId;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.streamUrl = streamUrl;
    }

    Long getOrgId() {
        return orgId;
    }

    String getTitle() {
        return title;
    }

    Instant getScheduledAt() {
        return scheduledAt;
    }

    String getStreamUrl() {
        return streamUrl;
    }

    MeetingStatus getStatus() {
        return status;
    }

    void reschedule(String title, Instant scheduledAt, String streamUrl) {
        ensurePlanned("changed");
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.streamUrl = streamUrl;
    }

    /**
     * @param now current time; a meeting cannot be held before it starts
     */
    void markHeld(Instant now) {
        ensurePlanned("marked as held");
        if (scheduledAt.isAfter(now)) {
            throw new BusinessRuleException("The meeting has not started yet");
        }
        status = MeetingStatus.HELD;
    }

    void cancel() {
        ensurePlanned("cancelled");
        status = MeetingStatus.CANCELLED;
    }

    void ensurePlanned(String action) {
        if (status != MeetingStatus.PLANNED) {
            throw new BusinessRuleException("A meeting that is " + status.name().toLowerCase()
                    + " cannot be " + action);
        }
    }
}
