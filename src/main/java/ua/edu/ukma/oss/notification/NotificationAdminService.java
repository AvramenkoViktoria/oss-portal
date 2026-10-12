package ua.edu.ukma.oss.notification;

import java.util.List;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * Operational view of the notification pipeline for administrators.
 */
public interface NotificationAdminService {

    /**
     * Latest outbox entries, newest first.
     */
    List<OutboxEntryDto> recent(int limit);

    /**
     * Messages delivered by the stub messengers, newest first.
     */
    List<StubMessageDto> deliveredMessages();

    /**
     * Makes the stub of the messenger fail every send, to exercise retries.
     */
    void simulateOutage(Messenger messenger, boolean outage);

    boolean isOutageSimulated(Messenger messenger);

    /**
     * Delivers everything that is due now instead of waiting for the scheduler.
     *
     * @return number of entries attempted
     */
    int processDueNow();

    /**
     * Puts a failed entry back into the queue with a fresh attempt budget.
     */
    void requeue(Long entryId);
}
