package org.example.backend.controller;

import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.mindrot.jbcrypt.BCrypt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final String SECRET_KEY = "BardzoTajnyKluczZabezpieczajacyTokenyWymagajacyMinimum256Bitow!!";

    // Wzorce regex wracają do kontrolera
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Proste metody pomocnicze do walidacji
    private void validateRegistrationData(AuthRequest request) {
        if (request.getLogin() == null || !LOGIN_PATTERN.matcher(request.getLogin()).matches()) {
            // Rzucenie tego wyjątku natychmiast przerywa działanie i zwraca błąd 400
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Login contains forbidden characters or is too long. Permitted are alphanumerical characters, \".\" and \"_\", must be 3-20 characters long");
        }

        if (request.getPassword() == null || !PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be between 8-64 characters long, contain at least one uppercase letter, lowercase letter and a number");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) { // Usunięto @Valid

        // 1. Ręczna walidacja (jeśli coś jest nie tak, funkcja throwuje i kod poniżej się nie wykona)
        validateRegistrationData(request);

        // 2. Sprawdzenie czy użytkownik istnieje
        if (userRepository.findByLogin(request.getLogin()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login is already taken");
        }

        // 3. Zapis użytkownika
        User newUser = new User();
        newUser.setLogin(request.getLogin());
        newUser.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        newUser.setRole("USER");

        userRepository.save(newUser);
        return ResponseEntity.ok("Registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthRequest request) { // Usunięto @Valid

        // Ponownie zabezpieczamy wejście przed przepuszczeniem do bazy/BCrypta
        validateRegistrationData(request);

        Optional<User> userOpt = userRepository.findByLogin(request.getLogin());

        if (userOpt.isPresent() && BCrypt.checkpw(request.getPassword(), userOpt.get().getPassword())) {
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

        // Błąd 401 Unauthorized dla błędnych danych logowania
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login or password is incorrect");
    }
}