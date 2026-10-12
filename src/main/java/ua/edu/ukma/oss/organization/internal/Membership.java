package ua.edu.ukma.oss.organization.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import ua.edu.ukma.oss.shared.BaseEntity;

/**
 * A person's seat in an organization for one term of office.
 */
@Entity
@Table(name = "membership")
class Membership extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private String faculty;

    @Column(nullable = false)
    private String position;

    @Column(name = "term_start", nullable = false)
    private LocalDate termStart;

    @Column(name = "term_end")
    private LocalDate termEnd;

    protected Membership() {
    }

    Membership(Long userId, Long orgId, String faculty, String position, LocalDate termStart, LocalDate termEnd) {
        this.userId = userId;
        this.orgId = orgId;
        this.faculty = faculty;
        this.position = position;
        this.termStart = termStart;
        this.termEnd = termEnd;
    }

    Long getUserId() {
        return userId;
    }

    Long getOrgId() {
        return orgId;
    }

    String getFaculty() {
        return faculty;
    }

    String getPosition() {
        return position;
    }

    LocalDate getTermStart() {
        return termStart;
    }

    LocalDate getTermEnd() {
        return termEnd;
    }

    void endOn(LocalDate day) {
        this.termEnd = day;
    }
}
