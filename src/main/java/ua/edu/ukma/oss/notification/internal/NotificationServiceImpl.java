package ua.edu.ukma.oss.notification.internal;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingCancelledEvent;
import ua.edu.ukma.oss.meeting.MeetingScheduledEvent;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.notification.NotificationMessage;
import ua.edu.ukma.oss.notification.NotificationService;
import ua.edu.ukma.oss.protocol.ProtocolPublishedEvent;

/**
 * Stores messages in the outbox and turns domain events into announcements. The listeners run
 * synchronously inside the publisher's transaction, so the outbox row commits atomically with
 * the change that caused it.
 */
@Service
@Transactional
class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm",
            Locale.forLanguageTag("uk"));

    private final OutboxRepository outbox;
    private final MeetingService meetings;
    private final NotificationProperties properties;
    private final Clock clock;
    private final ZoneId zone;

    NotificationServiceImpl(OutboxRepository outbox, MeetingService meetings, NotificationProperties properties,
                            Clock clock) {
        this.outbox = outbox;
        this.meetings = meetings;
        this.properties = properties;
        this.clock = clock;
        this.zone = clock.getZone();
    }

    @Override
    public void notify(NotificationMessage message) {
        outbox.save(new OutboxEntry(message.orgId(), message.text(), clock.instant()));
    }

    @EventListener
    void onMeetingScheduled(MeetingScheduledEvent event) {
        StringBuilder text = new StringBuilder("📅 Анонс засідання «").append(event.title()).append("»: ")
                .append(format(event.scheduledAt()));
        if (event.streamUrl() != null) {
            text.append("\nТрансляція: ").append(event.streamUrl());
        }
        notify(new NotificationMessage(event.orgId(), text.toString()));
    }

    @EventListener
    void onMeetingCancelled(MeetingCancelledEvent event) {
        notify(new NotificationMessage(event.orgId(), "❌ Засідання «" + event.title() + "» скасовано"));
    }

    @EventListener
    void onProtocolPublished(ProtocolPublishedEvent event) {
        String title = meetings.get(event.meetingId()).title();
        String text = "📄 Опубліковано протокол засідання «" + title + "»\n"
                + properties.publicBaseUrl() + "/meetings/" + event.meetingId();
        notify(new NotificationMessage(event.orgId(), text));
    }

    private String format(Instant instant) {
        return WHEN.format(instant.atZone(zone));
    }
}
