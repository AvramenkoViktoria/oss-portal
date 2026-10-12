package ua.edu.ukma.oss.notification.internal;

import ua.edu.ukma.oss.notification.NotificationChannel;
import ua.edu.ukma.oss.organization.Messenger;

/**
 * A channel bound to one messenger. Registering a new bean of this type is all it takes to
 * support another messenger.
 */
interface MessengerChannel extends NotificationChannel {

    Messenger messenger();
}
