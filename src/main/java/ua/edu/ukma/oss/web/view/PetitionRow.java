package ua.edu.ukma.oss.web.view;

import ua.edu.ukma.oss.organization.OrganizationDto;
import ua.edu.ukma.oss.petition.PetitionDto;

/**
 * A petition with the body it is addressed to and, once scheduled, the meeting title.
 */
public record PetitionRow(PetitionDto petition, OrganizationDto org, String meetingTitle) {
}
