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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;
    
    @Value("${security.jwt.secret}")
    private String secretKey;

    public AuthController(UserRepository userRepository, TeamRepository teamRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (request.login == null || request.login.trim().isEmpty() ||
            request.password == null || request.password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login and password are required"));
        }

        if (userRepository.findByLogin(request.login).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login is already taken"));
        }

        if (request.teamId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Team is required"));
        }

        Optional<Team> teamOpt = teamRepository.findById(request.teamId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Selected team does not exist"));
        }

        Role defaultRole = roleRepository.findById("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        User newUser = new User();
        newUser.setLogin(request.login);
        newUser.setPassword(BCrypt.hashpw(request.password, BCrypt.gensalt()));
        newUser.setRole(defaultRole);
        newUser.setTeam(teamOpt.get());

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        if (request.login == null || request.login.trim().isEmpty() ||
            request.password == null || request.password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Login and password are required"));
        }

        Optional<User> userOpt = userRepository.findByLogin(request.login);

        if (userOpt.isPresent() && BCrypt.checkpw(request.password, userOpt.get().getPassword())) {
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
