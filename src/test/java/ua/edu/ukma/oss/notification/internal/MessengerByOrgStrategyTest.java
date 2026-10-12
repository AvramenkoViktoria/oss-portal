package ua.edu.ukma.oss.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.organization.OrgCode;
import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.organization.OrganizationService;

class MessengerByOrgStrategyTest {

    private final OrganizationService organizations = mock(OrganizationService.class);
    private final StubMessengerLog log = new StubMessengerLog(Clock.systemUTC());
    private final MessengerByOrgStrategy strategy = new MessengerByOrgStrategy(organizations,
            List.of(new WhatsAppChannel(log), new TelegramChannel(log)),
            new NotificationProperties(3, 1, Duration.ZERO, "http://x"));

    @Test
    void skSpeaksThroughWhatsAppAndKsThroughTelegram() {
        when(organizations.get(1L)).thenReturn(new OrganizationDto(1L, OrgCode.SK, "SK", Messenger.WHATSAPP, "wa"));
        when(organizations.get(2L)).thenReturn(new OrganizationDto(2L, OrgCode.KS, "KS", Messenger.TELEGRAM, "tg"));

        strategy.resolve(1L).send("wa", "hello sk");
        strategy.resolve(2L).send("tg", "hello ks");

        assertThat(log.messages()).extracting(m -> m.messenger())
                .containsExactly(Messenger.TELEGRAM, Messenger.WHATSAPP);
    }
}
