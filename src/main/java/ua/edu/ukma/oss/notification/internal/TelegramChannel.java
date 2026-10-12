package ua.edu.ukma.oss.notification.internal;

import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.notification.SendResult;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * Stand-in for the Telegram Bot API: records the message instead of calling the network.
 */
@Component
class TelegramChannel implements MessengerChannel {

    private final StubMessengerLog log;

    TelegramChannel(StubMessengerLog log) {
        this.log = log;
    }

    @Override
    public Messenger messenger() {
        return Messenger.TELEGRAM;
    }

    @Override
    public SendResult send(String channelId, String text) {
        if (log.isOutage(Messenger.TELEGRAM)) {
            return SendResult.failed("Telegram stub: simulated outage");
        }
        log.record(Messenger.TELEGRAM, channelId, text);
        return SendResult.ok();
    }
}
