package ua.edu.ukma.oss.shared;

/**
 * Raised when an operation violates a domain rule, e.g. an illegal lifecycle transition.
 * The message is safe to show to the end user.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
