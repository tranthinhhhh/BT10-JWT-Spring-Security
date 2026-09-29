package vn.edu.hcmute.jwt.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.edu.hcmute.jwt.entity.AppUser;
import vn.edu.hcmute.jwt.entity.Role;
import vn.edu.hcmute.jwt.repository.UserRepository;

@Component
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password,
            @Value("${app.admin.full-name}") String fullName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(password)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("ADMIN_EMAIL and ADMIN_PASSWORD must be set together");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setEmail(normalizedEmail);
            admin.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : "Administrator");
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            return userRepository.save(admin);
        });
    }
}
