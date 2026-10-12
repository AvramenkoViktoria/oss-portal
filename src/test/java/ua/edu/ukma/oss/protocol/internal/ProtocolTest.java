package ua.edu.ukma.oss.protocol.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class ProtocolTest {

    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    private Protocol draftWithFile() {
        Protocol p = new Protocol(1L, 1L, null);
        p.attachFile("key.pdf", "minutes.pdf");
        return p;
    }

    @Test
    void walksDraftApprovedPublished() {
        Protocol p = draftWithFile();

        p.approve(10L, NOW);
        assertThat(p.getStatus()).isEqualTo(ProtocolStatus.APPROVED);
        assertThat(p.getApprovedBy()).isEqualTo(10L);
        assertThat(p.getApprovedAt()).isEqualTo(NOW);

        p.publish(11L, NOW.plusSeconds(5));
        assertThat(p.getStatus()).isEqualTo(ProtocolStatus.PUBLISHED);
        assertThat(p.getPublishedBy()).isEqualTo(11L);
    }

    @Test
    void cannotApproveWithoutDocument() {
        Protocol p = new Protocol(1L, 1L, null);

        assertThatThrownBy(() -> p.approve(10L, NOW)).isInstanceOf(BusinessRuleException.class);
        assertThat(p.getStatus()).isEqualTo(ProtocolStatus.DRAFT);
    }

    @Test
    void cannotSkipApproval() {
        assertThatThrownBy(() -> draftWithFile().publish(10L, NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotApproveTwiceOrRepublish() {
        Protocol p = draftWithFile();
        p.approve(10L, NOW);
        assertThatThrownBy(() -> p.approve(10L, NOW)).isInstanceOf(BusinessRuleException.class);

        p.publish(10L, NOW);
        assertThatThrownBy(() -> p.publish(10L, NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void documentIsLockedAfterApproval() {
        Protocol p = draftWithFile();
        p.approve(10L, NOW);

        assertThatThrownBy(() -> p.attachFile("other.pdf", "other.pdf")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void replacingDocumentReturnsPreviousKey() {
        Protocol p = draftWithFile();

        assertThat(p.attachFile("new.pdf", "new.pdf")).isEqualTo("key.pdf");
    }
}
