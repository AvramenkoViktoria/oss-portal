package ua.edu.ukma.oss.protocol;

/**
 * One precondition of approving a protocol, shown to editors as a checklist.
 *
 * @param code        stable identifier
 * @param description human-readable condition
 * @param met         whether the condition currently holds
 */
public record Requirement(String code, String description, boolean met) {
}
