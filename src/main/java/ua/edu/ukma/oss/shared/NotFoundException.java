package ua.edu.ukma.oss.shared;

/**
 * Raised when a requested aggregate does not exist.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, Object id) {
        super(what + " not found: " + id);
    }
}
