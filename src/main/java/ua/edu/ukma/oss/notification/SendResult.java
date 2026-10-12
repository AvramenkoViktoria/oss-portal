package ua.edu.ukma.oss.notification;

/**
 * Outcome of one delivery attempt.
 *
 * @param error failure description, {@code null} on success
 */
public record SendResult(boolean success, String error) {

    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult failed(String error) {
        return new SendResult(false, error);
    }
}
