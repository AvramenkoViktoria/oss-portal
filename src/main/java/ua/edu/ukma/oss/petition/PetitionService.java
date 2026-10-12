package ua.edu.ukma.oss.petition;

import java.time.Instant;
import java.util.List;

/**
 * Public API of the petitions module.
 */
public interface PetitionService {

    PetitionDto submit(SubmitCmd cmd);

    PetitionDto get(Long petitionId);

    /**
     * Binds a submitted petition to a planned meeting of the same body. With
     * {@link PetitionLink#AGENDA_ITEM} an agenda item is added to the meeting.
     */
    PetitionDto attach(Long petitionId, Long meetingId, PetitionLink link);

    /**
     * Creates a new meeting dedicated to the petition and binds the petition to it.
     */
    PetitionDto scheduleSeparateMeeting(Long petitionId, String meetingTitle, Instant scheduledAt, String streamUrl);

    PetitionDto reject(Long petitionId, String reason);

    /**
     * Petitions addressed to the body, newest first.
     */
    List<PetitionDto> byOrg(Long orgId);

    List<PetitionDto> byAuthor(Long authorId);

    List<PetitionDto> byMeeting(Long meetingId);
}
