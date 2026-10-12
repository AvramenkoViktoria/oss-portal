package ua.edu.ukma.oss.notification.internal;

import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.notification.SendResult;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * Stand-in for the WhatsApp Business API: records the message instead of calling the network.
 */
@Component
class WhatsAppChannel implements MessengerChannel {

    private final StubMessengerLog log;

    WhatsAppChannel(StubMessengerLog log) {
        this.log = log;
    }

    @Override
    public Messenger messenger() {
        return Messenger.WHATSAPP;
    }

    @Override
    public SendResult send(String channelId, String text) {
        if (log.isOutage(Messenger.WHATSAPP)) {
            return SendResult.failed("WhatsApp stub: simulated outage");
        }
        log.record(Messenger.WHATSAPP, channelId, text);
        return SendResult.ok();
    }
}
