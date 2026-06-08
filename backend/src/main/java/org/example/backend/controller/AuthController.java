package org.example.backend.controller;

import org.example.backend.model.Role;
import org.example.backend.model.Team;
import org.example.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.mindrot.jbcrypt.BCrypt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;
    
    @Value("${security.jwt.secret}")
    private String secretKey;

    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");

    public AuthController(UserRepository userRepository, TeamRepository teamRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.roleRepository = roleRepository;
    }

    // Proste metody pomocnicze do walidacji
    private ResponseEntity<?> validateRegistrationData(AuthRequest request) {
        if (request.getLogin() == null || !LOGIN_PATTERN.matcher(request.getLogin()).matches()) {
            // Rzucenie tego wyjątku natychmiast przerywa działanie i zwraca błąd 400
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "\"Login contains forbidden characters or is too long. Permitted are alphanumerical characters, \\\".\\\" and \\\"_\\\", must be 3-20 characters long\"")
                    );
        }

        if (request.getPassword() == null || !PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "\"Password must be between 8-64 characters long, contain at least one uppercase letter, lowercase letter and a number\"")
            );

        }
        return null;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {

        ResponseEntity<?> valid = validateRegistrationData(request);
        if (valid != null){
            return valid;
        }
        if (request.getLogin() == null || request.getLogin().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login and password are required"));
        }

        if (userRepository.findByLogin(request.getLogin()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login is already taken"));
        }

        if (request.getTeamId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Team is required"));
        }

        Optional<Team> teamOpt = teamRepository.findById(request.getTeamId());
        if (teamOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Selected team does not exist"));
        }

        Role defaultRole = roleRepository.findById("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        User newUser = new User();
        newUser.setLogin(request.getLogin());
        newUser.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        newUser.setRole(defaultRole);
        newUser.setTeam(teamOpt.get());

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        ResponseEntity<?> valid = validateRegistrationData(request);
        if (valid != null){
            return valid;
        }
        if (request.getLogin() == null || request.getLogin().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login and password are required"));
        }

        Optional<User> userOpt = userRepository.findByLogin(request.getLogin());


        if (userOpt.isPresent() && BCrypt.checkpw(request.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();

            String token = Jwts.builder()
                    .setSubject(user.getId().toString())
                    .claim("role", user.getRole().getName())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day expiration
                    .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                    .compact();

            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login or password is incorrect"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // logout happens client-side, so we only return success
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
