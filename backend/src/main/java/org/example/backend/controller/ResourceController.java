package org.example.backend.controller;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.example.backend.model.Resource;
import org.example.backend.model.User;
import org.example.backend.repository.ResourceRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.stream.Collectors;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;


    // Zabezpiecza tytuł: od 3 do 100 znaków, całkowity zakaz nawiasów < i >
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[^<>]{3,100}$");

    public ResourceController(ResourceRepository resourceRepository, UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // Response DTO containing authorization details for the frontend
    public static class ResourceResponseDto {
        public Long id;
        public String title;
        public String description;
        public Long authorId;
        public String authorLogin;
        public String authorTeamName;
        public LocalDateTime creationDate;
        public boolean isPrivate;
        public boolean canEdit;
        public boolean canDelete;
    }

    // GET posty (z grupy + moje + dla wszystkich; posortowane wedlug daty) + max 15 na zapytanie
    @GetMapping
    public ResponseEntity<?> getResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }

            User currentUser = currentUserOpt.get();
            Long teamId = currentUser.getTeam() != null ? currentUser.getTeam().getId() : null;
            String currentUserRole = currentUser.getRole().getName();
            boolean isAdmin = currentUserRole.equals("ROLE_ADMIN");

            // Cap the page size at 15
            int pageSize = Math.min(size, 15);

            List<Resource> resources = resourceRepository.findVisibleResources(
                    currentUserId,
                    teamId,
                    isAdmin,
                    PageRequest.of(page, pageSize)
            );

            List<ResourceResponseDto> dtos = resources.stream().map(resource -> {
                ResourceResponseDto dto = new ResourceResponseDto();
                dto.id = resource.getId();
                dto.title = resource.getTitle();
                dto.description = resource.getDescription();
                dto.authorId = resource.getAuthorId();
                dto.creationDate = resource.getCreationDate();
                dto.isPrivate = resource.isPrivate();

                // Fetch author details
                Optional<User> authorOpt = userRepository.findById(resource.getAuthorId());
                if (authorOpt.isPresent()) {
                    User author = authorOpt.get();
                    dto.authorLogin = author.getLogin();
                    dto.authorTeamName = author.getTeam() != null ? author.getTeam().getNazwa() : "No Team";
                } else {
                    dto.authorLogin = "Deleted User";
                    dto.authorTeamName = "No Team";
                }

                // Determine frontend actions permissions
                boolean isAuthor = resource.getAuthorId().equals(currentUserId);
                
                boolean isTeamMod = false;
                if (currentUserRole.equals("ROLE_MOD") && authorOpt.isPresent()) {
                    User author = authorOpt.get();
                    if (author.getTeam() != null && currentUser.getTeam() != null) {
                        isTeamMod = author.getTeam().getId().equals(currentUser.getTeam().getId());
                    }
                }

                dto.canEdit = isAuthor || isAdmin;
                dto.canDelete = isAuthor || isAdmin || isTeamMod;

                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    // INSERT post (id_usera, title, description, bool czy do grupy czy do wszystkich)
    @PostMapping
    public ResponseEntity<?> createResource(@RequestBody ResourceRequest request, Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            
            if (request.getTitle() == null || request.getTitle().trim().isEmpty() ||
                request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Title and description are required"));
            }

            Resource resource = new Resource();
            resource.setTitle(request.getTitle());
            resource.setDescription(request.getDescription());
            resource.setAuthorId(currentUserId);
            resource.setPrivate(request.getPrivate() != null && request.getPrivate());

            resourceRepository.save(resource);
            return ResponseEntity.ok(Map.of("message", "Created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error creating post"));
        }
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
    private ResponseEntity<?> validateResourceData(ResourceRequest request) {
        if (request.getTitle() == null || !TITLE_PATTERN.matcher(request.getTitle()).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Title must be between 3 and 100 characters long and cannot contain HTML tags (< or >)")
            );
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty() || request.getDescription().length() > 2000) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "\"Content cannot be empty and must not exceed 2000 characters\"")
            );
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> createResource(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ResourceRequest request) {

        // 1. Walidacja tożsamości
        Claims claims = verifyToken(authHeader);
        Long userId = Long.parseLong(claims.getSubject());

        // 2. Jawna walidacja struktury danych wejściowych
        ResponseEntity<?> valid = validateResourceData(request);
        if (valid != null){
            return valid;
        }

        // 3. Budowa i zapis obiektu
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setAuthorId(userId);

        resourceRepository.save(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body("Resource created successfully");
    }

    // EDIT post (id, title, description)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequest request,
            Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            String roleName = authentication.getAuthorities().iterator().next().getAuthority();
            boolean isAdmin = roleName.equals("ROLE_ADMIN");

            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Post not found"));
            }

            Resource resource = resourceOpt.get();
            boolean isAuthor = resource.getAuthorId().equals(currentUserId);

            // Only author or ADMIN can edit
            if (!isAuthor && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No permission to edit this post"));
            }

            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                resource.setTitle(request.getTitle());
            }
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                resource.setDescription(request.getDescription());
            }

            resourceRepository.save(resource);
            return ResponseEntity.ok(Map.of("message", "Updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    // DELETE post (id)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id, Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }

            User currentUser = currentUserOpt.get();
            String currentUserRole = currentUser.getRole().getName();
            boolean isAdmin = currentUserRole.equals("ROLE_ADMIN");

            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Post not found"));
            }

            Resource resource = resourceOpt.get();
            boolean isAuthor = resource.getAuthorId().equals(currentUserId);

            // Check if moderator of the same team
            boolean isTeamMod = false;
            if (currentUserRole.equals("ROLE_MOD")) {
                Optional<User> authorOpt = userRepository.findById(resource.getAuthorId());
                if (authorOpt.isPresent()) {
                    User author = authorOpt.get();
                    if (author.getTeam() != null && currentUser.getTeam() != null) {
                        isTeamMod = author.getTeam().getId().equals(currentUser.getTeam().getId());
                    }
                }
            }

            if (isAuthor || isAdmin || isTeamMod) {
                resourceRepository.delete(resource);
                return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No permission to delete this post"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }
}