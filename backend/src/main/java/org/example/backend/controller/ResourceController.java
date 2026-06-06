package org.example.backend.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.example.backend.model.Resource;
import org.example.backend.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final String SECRET_KEY = "BardzoTajnyKluczZabezpieczajacyTokenyWymagajacyMinimum256Bitow!!";

    // Zabezpiecza tytuł: od 3 do 100 znaków, całkowity zakaz nawiasów < i >
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[^<>]{3,100}$");

    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    // Ekstrakcja i weryfikacja tokena z własną obsługą wyjątków
    private Claims verifyToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token format");
        }

        String token = authHeader.substring(7);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | SignatureException | IllegalArgumentException e) {
            // Bezpiecznie wyłapuje specyficzne błędy biblioteki jjwt i przekuwa je w czytelny błąd 401
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is invalid or expired");
        }
    }

    // Jawna metoda walidująca zasoby chroniąca przed XSS i DoS
    private void validateResourceData(ResourceRequest request) {
        if (request.getTitle() == null || !TITLE_PATTERN.matcher(request.getTitle()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Title must be between 3 and 100 characters long and cannot contain HTML tags (< or >)");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty() || request.getContent().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Content cannot be empty and must not exceed 2000 characters");
        }
    }

    @PostMapping
    public ResponseEntity<String> createResource(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ResourceRequest request) {

        // 1. Walidacja tożsamości
        Claims claims = verifyToken(authHeader);
        Long userId = Long.parseLong(claims.getSubject());

        // 2. Jawna walidacja struktury danych wejściowych
        validateResourceData(request);

        // 3. Budowa i zapis obiektu
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setContent(request.getContent());
        resource.setAuthorId(userId);

        resourceRepository.save(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body("Resource created successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResource(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        // 1. Walidacja tożsamości
        Claims claims = verifyToken(authHeader);
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        // 2. Walidacja istnienia zasobu
        Optional<Resource> resourceOpt = resourceRepository.findById(id);
        if (resourceOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        Resource resource = resourceOpt.get();

        // 3. Autoryzacja operacji: RBAC (ADMIN) lub ABAC (Autor)
        if ("ADMIN".equals(role) || resource.getAuthorId().equals(userId)) {
            resourceRepository.delete(resource);
            return ResponseEntity.ok("Resource deleted successfully");
        } else {
            // Kod 403 Forbidden dla pomyślnie uwierzytelnionych użytkowników bez praw dostępu
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this resource");
        }
    }
}