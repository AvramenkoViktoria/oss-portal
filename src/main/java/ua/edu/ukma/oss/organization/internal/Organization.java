package ua.edu.ukma.oss.organization.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.shared.BaseEntity;

/**
 * A student body. Rows are reference data created by the schema migration.
 */
@Entity
@Table(name = "organization")
class Organization extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private OrgCode code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Messenger messenger;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    protected Organization() {
    }

    OrgCode getCode() {
        return code;
    }

    String getName() {
        return name;
    }

    Messenger getMessenger() {
        return messenger;
    }

    String getChannelId() {
        return channelId;
    }

    void redirectTo(Messenger messenger, String channelId) {
        this.messenger = messenger;
        this.channelId = channelId;
    }
}
