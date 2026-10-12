package ua.edu.ukma.oss.protocol.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BaseEntity;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * Minutes of one meeting. The status field is never written from outside: transitions go
 * through {@link #approve} and {@link #publish}, which delegate to the current {@link ProtocolState}.
 */
@Entity
@Table(name = "protocol")
class Protocol extends BaseEntity {

    @Column(name = "meeting_id", nullable = false, unique = true)
    private Long meetingId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolStatus status = ProtocolStatus.DRAFT;

    @Column(name = "file_key")
    private String fileKey;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    protected Protocol() {
    }

    Protocol(Long meetingId, Long orgId, String fileKey) {
        this.meetingId = meetingId;
        this.orgId = orgId;
        this.fileKey = fileKey;
    }

    Long getMeetingId() {
        return meetingId;
    }

    Long getOrgId() {
        return orgId;
    }

    ProtocolStatus getStatus() {
        return status;
    }

    String getFileKey() {
        return fileKey;
    }

    String getFileName() {
        return fileName;
    }

    Instant getApprovedAt() {
        return approvedAt;
    }

    Long getApprovedBy() {
        return approvedBy;
    }

    Instant getPublishedAt() {
        return publishedAt;
    }

    Long getPublishedBy() {
        return publishedBy;
    }

    boolean hasFile() {
        return fileKey != null;
    }

    /**
     * Replaces the attached document; allowed only while the protocol is a draft.
     *
     * @return the previous storage key, or {@code null}, so the caller can clean it up
     */
    String attachFile(String key, String name) {
        if (!state().editable()) {
            throw new BusinessRuleException("The document of an approved or published protocol cannot be replaced");
        }
        String previous = this.fileKey;
        this.fileKey = key;
        this.fileName = name;
        return previous;
    }

    void approve(Long actorId, Instant now) {
        ProtocolState next = state().approve();
        if (!hasFile()) {
            throw new BusinessRuleException("Attach the protocol document before approving");
        }
        this.status = next.status();
        this.approvedAt = now;
        this.approvedBy = actorId;
    }

    void publish(Long actorId, Instant now) {
        this.status = state().publish().status();
        this.publishedAt = now;
        this.publishedBy = actorId;
    }

    boolean isEditable() {
        return state().editable();
    }

    private ProtocolState state() {
        return ProtocolStates.of(status);
    }
}
