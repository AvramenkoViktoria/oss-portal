package ua.edu.ukma.oss.organization;

/**
 * Read model of an organization.
 */
public record OrganizationDto(Long id, OrgCode code, String name, Messenger messenger, String channelId) {
}
