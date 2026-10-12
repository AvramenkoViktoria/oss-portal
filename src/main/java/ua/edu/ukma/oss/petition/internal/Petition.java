package ua.edu.ukma.oss.petition.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.shared.BaseEntity;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * A petition. All status changes are guarded transitions of this aggregate.
 */
@Entity
@Table(name = "petition")
class Petition extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetitionStatus status = PetitionStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type")
    private PetitionLink link;

    @Column(name = "meeting_id")
    private Long meetingId;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    protected Petition() {
    }

    Petition(String title, String text, Long authorId, Long orgId) {
        this.title = title;
        this.text = text;
        this.authorId = authorId;
        this.orgId = orgId;
    }

    String getTitle() {
        return title;
    }

    String getText() {
        return text;
    }

    Long getAuthorId() {
        return authorId;
    }

    Long getOrgId() {
        return orgId;
    }

    PetitionStatus getStatus() {
        return status;
    }

    PetitionLink getLink() {
        return link;
    }

    Long getMeetingId() {
        return meetingId;
    }

    String getRejectionReason() {
        return rejectionReason;
    }

    void schedule(Long meetingId, PetitionLink link) {
        require(PetitionStatus.SUBMITTED, "scheduled");
        this.status = PetitionStatus.SCHEDULED;
        this.meetingId = meetingId;
        this.link = link;
    }

    void consider() {
        require(PetitionStatus.SCHEDULED, "marked as considered");
        this.status = PetitionStatus.CONSIDERED;
    }

    void reject(String reason) {
        require(PetitionStatus.SUBMITTED, "rejected");
        this.status = PetitionStatus.REJECTED;
        this.rejectionReason = reason;
    }

    /**
     * Returns a scheduled petition to the queue after its meeting was cancelled.
     */
    void unschedule() {
        require(PetitionStatus.SCHEDULED, "returned to the queue");
        this.status = PetitionStatus.SUBMITTED;
        this.meetingId = null;
        this.link = null;
    }

    private void require(PetitionStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("A petition that is " + status.name().toLowerCase()
                    + " cannot be " + action);
        }
    }
}
