package org.example.backend.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.example.backend.model.Resource;
import org.example.backend.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final String SECRET_KEY = "BardzoTajnyKluczZabezpieczajacyTokenyWymagajacyMinimum256Bitow!!";

    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    private Claims verifyToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("no token or bad format");
        }

        // remove "bearer "
        String token = authHeader.substring(7);

        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // has to be logged in
    @PostMapping
    public ResponseEntity<?> createResource(@RequestHeader("Authorization") String authHeader, @RequestBody ResourceRequest request) {
        try {
            Claims claims = verifyToken(authHeader);
            Long userId = Long.parseLong(claims.getSubject());

            Resource resource = new Resource();
            resource.setTitle(request.title);
            resource.setContent(request.content);
            resource.setAuthorId(userId);

            resourceRepository.save(resource);
            return ResponseEntity.ok("created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("outdated or wrong token");
        }
    }

    // (RBAC + ABAC)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            Claims claims = verifyToken(authHeader);
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = resourceOpt.get();

            if (role.equals("ADMIN") || resource.getAuthorId().equals(userId)) {
                resourceRepository.delete(resource);
                return ResponseEntity.ok("resource deleted");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no permission");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("outdated or wrong token");
        }
    }
}
