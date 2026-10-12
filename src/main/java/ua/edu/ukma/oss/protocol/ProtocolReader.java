package ua.edu.ukma.oss.protocol;

import java.util.List;
import java.util.Optional;

/**
 * Read side of the module, used by the public part of the site.
 */
public interface ProtocolReader {

    ProtocolDto get(Long protocolId);

    Optional<ProtocolDto> byMeeting(Long meetingId);

    /**
     * Published protocols of the body, most recently published first.
     */
    List<ProtocolDto> published(Long orgId);

    /**
     * Protocols of the body in every status, for the editors' cabinet.
     */
    List<ProtocolDto> all(Long orgId);

    /**
     * Preconditions of approval with their current state, built-in ones first.
     */
    List<Requirement> approvalRequirements(Long protocolId);

    /**
     * Opens the attached document, if any.
     */
    Optional<ProtocolFile> file(Long protocolId);
}
