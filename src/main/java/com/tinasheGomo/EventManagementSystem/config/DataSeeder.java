package com.tinasheGomo.EventManagementSystem.config;

import com.tinasheGomo.EventManagementSystem.entity.organization.OrganizationEntity;
import com.tinasheGomo.EventManagementSystem.entity.user.UserEntity;
import com.tinasheGomo.EventManagementSystem.enums.UserRole;
import com.tinasheGomo.EventManagementSystem.repository.organization.OrganizationRepository;
import com.tinasheGomo.EventManagementSystem.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the demo organization and the first ADMIN account when the database
 * has no user with the configured email yet.
 *
 * <p>No credential is hardcoded. {@code application.properties} maps
 * {@code app.seed.admin.email} from {@code SEED_ADMIN_EMAIL} and
 * {@code app.seed.admin.password} from {@code SEED_ADMIN_PASSWORD}. When the
 * password is unset or blank the seeder does nothing, so a deployment that does
 * not want a seeded account simply leaves the variable out. Existing databases
 * are never touched.</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminDisplayName;
    private final String adminPhone;
    private final String adminPassword;

    public DataSeeder(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin.email:}") String adminEmail,
            @Value("${app.seed.admin.display-name:Tinashe Gomo}") String adminDisplayName,
            @Value("${app.seed.admin.phone:0774964677}") String adminPhone,
            @Value("${app.seed.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminDisplayName = adminDisplayName;
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(adminPassword) || !StringUtils.hasText(adminEmail)) {
            log.info("SEED_ADMIN_EMAIL/SEED_ADMIN_PASSWORD not set - skipping demo admin seeding.");
            return;
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        OrganizationEntity org = new OrganizationEntity();
        org.setName("EventFlow Demo");
        org.setOwnerId("seeded");
        org = organizationRepository.save(org);

        UserEntity admin = new UserEntity();
        admin.setUid(java.util.UUID.randomUUID().toString());
        admin.setEmail(adminEmail);
        admin.setDisplayName(adminDisplayName);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setPhone(adminPhone);
        admin.setOrganization(org);
        admin = userRepository.save(admin);

        org.setOwnerId(admin.getId());
        organizationRepository.save(org);

        // Never log the password - it came from SEED_ADMIN_PASSWORD and stays there.
        log.info("Seeded demo admin account: {}", adminEmail);
    }
}
