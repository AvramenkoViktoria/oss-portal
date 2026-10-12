package ua.edu.ukma.oss.notification.internal;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import ua.edu.ukma.oss.notification.StubMessageDto;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * Shared state of the stub messengers: a bounded in-memory log of "delivered" messages and
 * per-messenger outage switches.
 */
@Component
class StubMessengerLog {

    private static final int CAPACITY = 200;

    private final Deque<StubMessageDto> messages = new ArrayDeque<>();
    private final Set<Messenger> outages = EnumSet.noneOf(Messenger.class);
    private final Clock clock;

    StubMessengerLog(Clock clock) {
        this.clock = clock;
    }

    synchronized void record(Messenger messenger, String channelId, String text) {
        messages.addFirst(new StubMessageDto(messenger, channelId, text, clock.instant()));
        while (messages.size() > CAPACITY) {
            messages.removeLast();
        }
    }

    synchronized List<StubMessageDto> messages() {
        return List.copyOf(messages);
    }

    synchronized void setOutage(Messenger messenger, boolean outage) {
        if (outage) {
            outages.add(messenger);
        } else {
            outages.remove(messenger);
        }
    }

    synchronized boolean isOutage(Messenger messenger) {
        return outages.contains(messenger);
    }
}
