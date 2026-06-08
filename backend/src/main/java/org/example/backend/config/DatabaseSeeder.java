package org.example.backend.config;

import org.example.backend.model.Role;
import org.example.backend.model.Team;
import org.example.backend.model.User;
import org.example.backend.model.Resource;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.ResourceRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(
            RoleRepository roleRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            ResourceRepository resourceRepository) {
        return args -> {
            Role roleUser = null;
            Role roleMod = null;
            Role roleAdmin = null;

            if (roleRepository.count() == 0) {
                roleUser = roleRepository.save(new Role("ROLE_USER"));
                roleMod = roleRepository.save(new Role("ROLE_MOD"));
                roleAdmin = roleRepository.save(new Role("ROLE_ADMIN"));
            } else {
                roleUser = roleRepository.findById("ROLE_USER").orElse(null);
                roleMod = roleRepository.findById("ROLE_MOD").orElse(null);
                roleAdmin = roleRepository.findById("ROLE_ADMIN").orElse(null);
            }

            Team realMadrid = null;
            Team fcBarcelona = null;
            Team manUnited = null;
            Team bayern = null;
            Team arsenal = null;

            if (teamRepository.count() == 0) {
                realMadrid = teamRepository.save(new Team("Real Madrid"));
                fcBarcelona = teamRepository.save(new Team("FC Barcelona"));
                manUnited = teamRepository.save(new Team("Manchester United"));
                bayern = teamRepository.save(new Team("Bayern Munich"));
                arsenal = teamRepository.save(new Team("Arsenal"));
            } else {
                realMadrid = teamRepository.findByName("Real Madrid").orElse(null);
                fcBarcelona = teamRepository.findByName("FC Barcelona").orElse(null);
                manUnited = teamRepository.findByName("Manchester United").orElse(null);
                bayern = teamRepository.findByName("Bayern Munich").orElse(null);
                arsenal = teamRepository.findByName("Arsenal").orElse(null);
            }

            User admin = null;
            User realUser = null;
            User realMod = null;
            User barcaUser = null;
            User barcaMod = null;

            if (userRepository.count() == 0) {
                admin = new User();
                admin.setLogin("admin");
                admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
                admin.setRole(roleAdmin);
                admin.setTeam(arsenal);
                admin = userRepository.save(admin);

                realUser = new User();
                realUser.setLogin("real_user");
                realUser.setPassword(BCrypt.hashpw("user123", BCrypt.gensalt()));
                realUser.setRole(roleUser);
                realUser.setTeam(realMadrid);
                realUser = userRepository.save(realUser);

                realMod = new User();
                realMod.setLogin("real_mod");
                realMod.setPassword(BCrypt.hashpw("mod123", BCrypt.gensalt()));
                realMod.setRole(roleMod);
                realMod.setTeam(realMadrid);
                realMod = userRepository.save(realMod);

                barcaUser = new User();
                barcaUser.setLogin("barca_user");
                barcaUser.setPassword(BCrypt.hashpw("user123", BCrypt.gensalt()));
                barcaUser.setRole(roleUser);
                barcaUser.setTeam(fcBarcelona);
                barcaUser = userRepository.save(barcaUser);

                barcaMod = new User();
                barcaMod.setLogin("barca_mod");
                barcaMod.setPassword(BCrypt.hashpw("mod123", BCrypt.gensalt()));
                barcaMod.setRole(roleMod);
                barcaMod.setTeam(fcBarcelona);
                barcaMod = userRepository.save(barcaMod);
            } else {
                realUser = userRepository.findByLogin("real_user").orElse(null);
                barcaUser = userRepository.findByLogin("barca_user").orElse(null);
            }

            if (resourceRepository.count() == 0 && realUser != null && barcaUser != null) {
                Resource post1 = new Resource();
                post1.setTitle("Real Madrid in Champions League");
                post1.setDescription("Real Madrid reaches yet another Champions League final! What an incredible season.");
                post1.setAuthorId(realUser.getId());
                post1.setPrivate(false);
                post1.setCreationDate(LocalDateTime.now().minusHours(5));
                resourceRepository.save(post1);

                Resource post2 = new Resource();
                post2.setTitle("Real Madrid Secret Tactics");
                post2.setDescription("This tactics board contains confidential setups for the El Clasico match. Keep this inside the team.");
                post2.setAuthorId(realUser.getId());
                post2.setPrivate(true);
                post2.setCreationDate(LocalDateTime.now().minusHours(4));
                resourceRepository.save(post2);

                Resource post3 = new Resource();
                post3.setTitle("Barcelona stadium renovation");
                post3.setDescription("The renovation of Camp Nou is progressing smoothly. Expected reopening in winter.");
                post3.setAuthorId(barcaUser.getId());
                post3.setPrivate(false);
                post3.setCreationDate(LocalDateTime.now().minusHours(3));
                resourceRepository.save(post3);

                Resource post4 = new Resource();
                post4.setTitle("FC Barcelona board updates");
                post4.setDescription("Confidential discussion about the upcoming transfers and wage structure cap adjustments.");
                post4.setAuthorId(barcaUser.getId());
                post4.setPrivate(true);
                post4.setCreationDate(LocalDateTime.now().minusHours(2));
                resourceRepository.save(post4);
            }
        };
    }
}
