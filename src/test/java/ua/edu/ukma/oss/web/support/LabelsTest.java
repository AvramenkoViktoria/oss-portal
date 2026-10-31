package ua.edu.ukma.oss.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.meeting.MeetingStatus;
import ua.edu.ukma.oss.notification.OutboxStatus;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.petition.PetitionLink;
import ua.edu.ukma.oss.petition.PetitionStatus;
import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.voting.VoteChoice;
import ua.edu.ukma.oss.voting.VoteResult;

class LabelsTest {

    @Test
    void everyEnumValueHasANonBlankLabel() {
        Arrays.stream(ProtocolStatus.values()).forEach(s -> {
            assertThat(Labels.protocolStatus(s)).isNotBlank();
            assertThat(Labels.protocolCss(s)).isEqualTo(s.name().toLowerCase());
        });
        Arrays.stream(MeetingStatus.values()).forEach(s -> {
            assertThat(Labels.meetingStatus(s)).isNotBlank();
            assertThat(Labels.meetingCss(s)).isNotBlank();
        });
        Arrays.stream(PetitionStatus.values()).forEach(s -> {
            assertThat(Labels.petitionStatus(s)).isNotBlank();
            assertThat(Labels.petitionCss(s)).isNotBlank();
        });
        Arrays.stream(PetitionLink.values()).forEach(l -> assertThat(Labels.petitionLink(l)).isNotBlank());
        Arrays.stream(VoteChoice.values()).forEach(c -> assertThat(Labels.choice(c)).isNotBlank());
        Arrays.stream(VoteResult.values()).forEach(r -> assertThat(Labels.voteResult(r)).isNotBlank());
        Arrays.stream(Messenger.values()).forEach(m -> assertThat(Labels.messenger(m)).isNotBlank());
        Arrays.stream(OutboxStatus.values()).forEach(s -> {
            assertThat(Labels.outboxStatus(s)).isNotBlank();
            assertThat(Labels.outboxCss(s)).isNotBlank();
        });
    }

    @Test
    void labelsAreInUkrainian() {
        assertThat(Labels.protocolStatus(ProtocolStatus.PUBLISHED)).isEqualTo("Опубліковано");
        assertThat(Labels.meetingStatus(MeetingStatus.CANCELLED)).isEqualTo("Скасовано");
        assertThat(Labels.petitionStatus(PetitionStatus.REJECTED)).isEqualTo("Відхилено");
        assertThat(Labels.voteResult(null)).isEqualTo("Голоси не внесено");
        assertThat(Labels.messenger(Messenger.WHATSAPP)).isEqualTo("WhatsApp");
    }

    @Test
    void initialsTakeUpToTwoCapitalLetters() {
        assertThat(Labels.initials("Іван Петренко")).isEqualTo("ІП");
        assertThat(Labels.initials("  іван  петрович  коваль ")).isEqualTo("ІП");
        assertThat(Labels.initials("Анна")).isEqualTo("А");
        assertThat(Labels.initials(" ")).isEqualTo("?");
        assertThat(Labels.initials(null)).isEqualTo("?");
    }
}
