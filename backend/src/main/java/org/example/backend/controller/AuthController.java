package org.example.backend.controller;

import org.example.backend.model.User;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final String SECRET_KEY = "BardzoTajnyKluczZabezpieczajacyTokenyWymagajacyMinimum256Bitow!!";

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        if (userRepository.findByLogin(request.login).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Login is already taken");
        }

        User newUser = new User();
        newUser.setLogin(request.login);
        newUser.setPassword(BCrypt.hashpw(request.password, BCrypt.gensalt()));
        newUser.setRole("USER");

        userRepository.save(newUser);
        return ResponseEntity.ok("Registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest request) {
        Optional<User> userOpt = userRepository.findByLogin(request.login);

        if (userOpt.isPresent() && BCrypt.checkpw(request.password, userOpt.get().getPassword())) {
            User user = userOpt.get();

            String token = Jwts.builder()
                    .setSubject(user.getId().toString())
                    .claim("role", user.getRole())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                    .compact();

            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login or password is incorrect");
    }
}
