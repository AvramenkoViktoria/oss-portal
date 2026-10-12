package ua.edu.ukma.oss.protocol.internal;

import ua.edu.ukma.oss.protocol.ProtocolStatus;

/**
 * State pattern for the protocol lifecycle: each state knows which transitions it allows
 * and what the next state is, so {@link Protocol} contains no status conditionals.
 */
interface ProtocolState {

    ProtocolStatus status();

    /**
     * Whether the content (attached document, votings) may still change.
     */
    boolean editable();

    ProtocolState approve();

    ProtocolState publish();
}
