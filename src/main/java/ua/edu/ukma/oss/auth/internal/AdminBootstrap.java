package ua.edu.ukma.oss.auth.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.ukma.oss.auth.Role;
import ua.edu.ukma.oss.auth.UserService;

/**
 * Creates the first administrator from {@code oss.bootstrap-admin.*} when none exists yet,
 * so a fresh deployment can be managed without touching the database.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private final UserService users;
    private final UserRepository repository;
    private final String email;
    private final String password;

    AdminBootstrap(UserService users, UserRepository repository,
                   @Value("${oss.bootstrap-admin.email:}") String email,
                   @Value("${oss.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.repository = repository;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || repository.existsByEmail(email.toLowerCase())) {
            return;
        }
        Long id = users.register(email, password, "Administrator").id();
        users.grant(id, Role.ADMIN, null);
    }
}
