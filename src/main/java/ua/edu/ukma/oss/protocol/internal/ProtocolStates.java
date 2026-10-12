package ua.edu.ukma.oss.protocol.internal;

import ua.edu.ukma.oss.protocol.ProtocolStatus;
import ua.edu.ukma.oss.shared.BusinessRuleException;

/**
 * The three concrete lifecycle states.
 */
final class ProtocolStates {

    static final ProtocolState DRAFT = new ProtocolState() {
        @Override
        public ProtocolStatus status() {
            return ProtocolStatus.DRAFT;
        }

        @Override
        public boolean editable() {
            return true;
        }

        @Override
        public ProtocolState approve() {
            return APPROVED;
        }

        @Override
        public ProtocolState publish() {
            throw new BusinessRuleException("A draft must be approved before it can be published");
        }
    };

    static final ProtocolState APPROVED = new ProtocolState() {
        @Override
        public ProtocolStatus status() {
            return ProtocolStatus.APPROVED;
        }

        @Override
        public boolean editable() {
            return false;
        }

        @Override
        public ProtocolState approve() {
            throw new BusinessRuleException("The protocol is already approved");
        }

        @Override
        public ProtocolState publish() {
            return PUBLISHED;
        }
    };

    static final ProtocolState PUBLISHED = new ProtocolState() {
        @Override
        public ProtocolStatus status() {
            return ProtocolStatus.PUBLISHED;
        }

        @Override
        public boolean editable() {
            return false;
        }

        @Override
        public ProtocolState approve() {
            throw new BusinessRuleException("The protocol is already published");
        }

        @Override
        public ProtocolState publish() {
            throw new BusinessRuleException("The protocol is already published");
        }
    };

    private ProtocolStates() {
    }

    static ProtocolState of(ProtocolStatus status) {
        return switch (status) {
            case DRAFT -> DRAFT;
            case APPROVED -> APPROVED;
            case PUBLISHED -> PUBLISHED;
        };
    }
}
