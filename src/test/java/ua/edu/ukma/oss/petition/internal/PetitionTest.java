package ua.edu.ukma.oss.petition.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;

class PetitionTest {

    private Petition petition() {
        return new Petition("Longer library hours", "Please keep it open until midnight", 1L, 1L);
    }

    @Test
    void goesThroughTheHappyPath() {
        Petition p = petition();

        p.schedule(7L, PetitionLink.AGENDA_ITEM);
        assertThat(p.getStatus()).isEqualTo(PetitionStatus.SCHEDULED);
        assertThat(p.getMeetingId()).isEqualTo(7L);

        p.consider();
        assertThat(p.getStatus()).isEqualTo(PetitionStatus.CONSIDERED);
    }

    @Test
    void cancelledMeetingReturnsPetitionToQueue() {
        Petition p = petition();
        p.schedule(7L, PetitionLink.SEPARATE_MEETING);

        p.unschedule();

        assertThat(p.getStatus()).isEqualTo(PetitionStatus.SUBMITTED);
        assertThat(p.getMeetingId()).isNull();
        assertThat(p.getLink()).isNull();
    }

    @Test
    void onlySubmittedPetitionCanBeRejected() {
        Petition p = petition();
        p.schedule(7L, PetitionLink.AGENDA_ITEM);

        assertThatThrownBy(() -> p.reject("no")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectedPetitionIsFinal() {
        Petition p = petition();
        p.reject("Out of competence");

        assertThat(p.getRejectionReason()).isEqualTo("Out of competence");
        assertThatThrownBy(() -> p.schedule(7L, PetitionLink.AGENDA_ITEM)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotBeConsideredWithoutSchedule() {
        assertThatThrownBy(() -> petition().consider()).isInstanceOf(BusinessRuleException.class);
    }
}
