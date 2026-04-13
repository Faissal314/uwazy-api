package com.uwazy.api;

import com.uwazy.api.course.Category;
import com.uwazy.api.course.CategoryRepository;
import com.uwazy.api.user.Role;
import com.uwazy.api.user.User;
import com.uwazy.api.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            Category dev = new Category();
            dev.setName("Developpement Web");
            dev.setDescription("Apprenez a creer des sites web");
            categoryRepository.save(dev);

            Category sec = new Category();
            sec.setName("Cybersecurite");
            sec.setDescription("Securisez vos applications");
            categoryRepository.save(sec);
        }

        if (categoryRepository.findByName("Bureautique (la suite office)").isEmpty()) {
            Category office = new Category();
            office.setName("Bureautique (la suite office)");
            office.setDescription("Maîtrisez Word, Excel, PowerPoint, etc.");
            categoryRepository.save(office);
        }

        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("Uwazy");
            admin.setEmail("admin@uwazy.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMINISTRATEUR);
            userRepository.save(admin);

            User teacher = new User();
            teacher.setFirstName("Formateur");
            teacher.setLastName("Test");
            teacher.setEmail("formateur@uwazy.com");
            teacher.setPassword(passwordEncoder.encode("formateur123"));
            teacher.setRole(Role.FORMATEUR);
            userRepository.save(teacher);
        }
    }
}
