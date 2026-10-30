package ua.edu.ukma.oss.notification.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.meeting.MeetingDto;
import ua.edu.ukma.oss.meeting.MeetingService;
import ua.edu.ukma.oss.notification.NotificationMessage;
import ua.edu.ukma.oss.notification.NotificationService;

/**
 * Queues a reminder in the body's messenger when a planned meeting is within an hour of starting. A meeting created
 * closer to its start than that is reminded on the next run. Marking the meeting and queueing the message happen in
 * one transaction, so a reminder is neither lost nor sent twice.
 */
@Component
class MeetingReminderJob {

    static final Duration LEAD = Duration.ofHours(1);

    private static final Logger log = LoggerFactory.getLogger(MeetingReminderJob.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("uk"));

    private final MeetingService meetings;
    private final NotificationService notifications;
    private final Clock clock;
    private final ZoneId zone;

    MeetingReminderJob(MeetingService meetings, NotificationService notifications, Clock clock) {
        this.meetings = meetings;
        this.notifications = notifications;
        this.clock = clock;
        this.zone = clock.getZone();
    }

    @Scheduled(fixedDelayString = "${oss.notification.reminder-interval:1m}")
    @Transactional
    public void remind() {
        remindDue();
    }

    /**
     * @return number of reminders queued
     */
    @Transactional
    public int remindDue() {
        var due = meetings.claimReminders(LEAD);
        due.forEach(m -> notifications.notify(new NotificationMessage(m.orgId(), text(m))));
        if (!due.isEmpty()) {
            log.info("Queued {} meeting reminder(s)", due.size());
        }
        return due.size();
    }

    private String text(MeetingDto m) {
        long minutes = Duration.between(clock.instant(), m.scheduledAt()).toMinutes();
        String when = minutes >= 55 ? "за годину" : "через " + Math.max(minutes, 1) + " хв";
        StringBuilder text = new StringBuilder("⏰ Нагадування: засідання «").append(m.title())
                .append("» розпочнеться ").append(when).append(", о ")
                .append(TIME.format(m.scheduledAt().atZone(zone)));
        if (m.streamUrl() != null) {
            text.append("\nТрансляція: ").append(m.streamUrl());
        }
        return text.toString();
    }
}
