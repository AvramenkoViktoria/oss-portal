package ua.edu.ukma.oss.petition;

/**
 * How a scheduled petition is bound to its meeting.
 */
public enum PetitionLink {
    /** The meeting is dedicated to this petition. */
    SEPARATE_MEETING,
    /** The petition is one item of an existing meeting's agenda. */
    AGENDA_ITEM
}
