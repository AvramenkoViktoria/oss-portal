package ua.edu.ukma.oss.petition;

/**
 * Decides who may submit a petition to a body.
 */
public interface AuthorEligibilityPolicy {

    /**
     * @throws ua.edu.ukma.oss.shared.BusinessRuleException when the user may not submit
     */
    void ensureEligible(Long userId, Long orgId);
}
