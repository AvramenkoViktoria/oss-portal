package ua.edu.ukma.oss.protocol;

import java.io.InputStream;

/**
 * Write side of the module, used by the editors' cabinet.
 */
public interface ProtocolWriter {

    /**
     * Creates the draft for a held meeting.
     *
     * @param fileKey storage key of an already stored document, or {@code null}
     */
    ProtocolDto createDraft(Long meetingId, String fileKey);

    /**
     * Stores the document and attaches it to a draft, replacing a previous one.
     * Only {@code .pdf} and {@code .docx} files are accepted.
     */
    ProtocolDto attachFile(Long protocolId, String fileName, InputStream content);

    /**
     * Approves a draft once all {@link Requirement requirements} are met.
     */
    ProtocolDto approve(Long protocolId, Long actorId);

    /**
     * Publishes an approved protocol and emits {@link ProtocolPublishedEvent}.
     */
    ProtocolDto publish(Long protocolId, Long actorId);
}
