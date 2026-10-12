package ua.edu.ukma.oss.auth;

/**
 * Raised on a failed login. The message deliberately does not reveal which part was wrong.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Неправильна електронна пошта або пароль");
    }
}
