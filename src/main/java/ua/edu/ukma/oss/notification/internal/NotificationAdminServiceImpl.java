package ua.edu.ukma.oss.notification.internal;

import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.notification.NotificationAdminService;
import ua.edu.ukma.oss.notification.OutboxEntryDto;
import ua.edu.ukma.oss.notification.OutboxStatus;
import ua.edu.ukma.oss.notification.StubMessageDto;
import ua.edu.ukma.oss.organization.Messenger;
import ua.edu.ukma.oss.shared.BusinessRuleException;
import ua.edu.ukma.oss.shared.NotFoundException;

@Service
@Transactional
class NotificationAdminServiceImpl implements NotificationAdminService {

    private final OutboxRepository outbox;
    private final StubMessengerLog stubLog;
    private final OutboxProcessor processor;
    private final Clock clock;

    NotificationAdminServiceImpl(OutboxRepository outbox, StubMessengerLog stubLog, OutboxProcessor processor,
                                 Clock clock) {
        this.outbox = outbox;
        this.stubLog = stubLog;
        this.processor = processor;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEntryDto> recent(int limit) {
        return outbox.findAllByOrderByIdDesc(PageRequest.of(0, limit)).stream()
                .map(e -> new OutboxEntryDto(e.getId(), e.getOrgId(), e.getPayload(), e.getStatus(), e.getAttempts(),
                        e.getNextAttemptAt(), e.getLastError(), e.getCreatedAt(), e.getSentAt()))
                .toList();
    }

    @Override
    public List<StubMessageDto> deliveredMessages() {
        return stubLog.messages();
    }

    @Override
    public void simulateOutage(Messenger messenger, boolean outage) {
        stubLog.setOutage(messenger, outage);
    }

    @Override
    public boolean isOutageSimulated(Messenger messenger) {
        return stubLog.isOutage(messenger);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int processDueNow() {
        return processor.processDue();
    }

    @Override
    public void requeue(Long entryId) {
        OutboxEntry entry = outbox.findById(entryId).orElseThrow(() -> new NotFoundException("Outbox entry", entryId));
        if (entry.getStatus() != OutboxStatus.FAILED) {
            throw new BusinessRuleException("У чергу можна повернути лише повідомлення, які не вдалося надіслати");
        }
        entry.requeue(clock.instant());
    }
}
