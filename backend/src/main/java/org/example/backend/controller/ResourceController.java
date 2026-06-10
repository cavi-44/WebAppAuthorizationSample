package org.example.backend.controller;
import org.example.backend.config.access_control.ABACService;
import org.example.backend.config.access_control.RBACService;
import org.example.backend.model.Resource;
import org.example.backend.model.User;
import org.example.backend.repository.ResourceRepository;
import org.example.backend.repository.UserRepository;
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
    private final RBACService rbacService;
    private final ABACService abacService;
    // Zabezpiecza tytuł: od 3 do 100 znaków, całkowity zakaz nawiasów < i >
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[^<>]{3,100}$");

    public ResourceController(ResourceRepository resourceRepository, UserRepository userRepository, RBACService rbacService, ABACService abacService) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.rbacService = rbacService;
        this.abacService = abacService;
    }

    // DTO
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

            boolean isAdmin = rbacService.validateRoles(authentication, "ROLE_ADMIN");


            List<Resource> resources = abacService.getVisibleResources(currentUser, isAdmin, page, size);


            List<ResourceResponseDto> dtos = resources.stream().map(resource -> {
                ResourceResponseDto dto = new ResourceResponseDto();
                dto.id = resource.getId();
                dto.title = resource.getTitle();
                dto.description = resource.getDescription();
                dto.authorId = resource.getAuthorId();
                dto.creationDate = resource.getCreationDate();
                dto.isPrivate = resource.isPrivate();

                Optional<User> authorOpt = userRepository.findById(resource.getAuthorId());
                if (authorOpt.isPresent()) {
                    User author = authorOpt.get();
                    dto.authorLogin = author.getLogin();
                    dto.authorTeamName = author.getTeam() != null ? author.getTeam().getName() : "No Team";
                } else {
                    dto.authorLogin = "Deleted User";
                    dto.authorTeamName = "No Team";
                }



                boolean isMod = rbacService.validateRoles(authentication, "ROLE_MOD");


                dto.canEdit = abacService.canEdit(currentUser, resource, isAdmin);
                dto.canDelete = abacService.canDelete(currentUser, resource, isAdmin, isMod);

                return dto;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createResource(@RequestBody ResourceRequest request, Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            

            ResponseEntity<?> validationError = validateResourceData(request);
            if (validationError != null) {
                return validationError;
            }

            Resource resource = new Resource();
            resource.setTitle(request.getTitle());
            resource.setDescription(request.getDescription());
            resource.setAuthorId(currentUserId);
            resource.setPrivate(request.getIsPrivate() != null && request.getIsPrivate());

            resourceRepository.save(resource);
            return ResponseEntity.ok(Map.of("message", "Resource created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error creating post"));
        }
    }


    private ResponseEntity<?> validateResourceData(ResourceRequest request) {
        if (request.getTitle() == null || !TITLE_PATTERN.matcher(request.getTitle()).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("message", "Title must be between 3 and 100 characters long and cannot contain HTML tags (< or >)")
            );
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty() || request.getDescription().length() > 2000) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("message", "Content cannot be empty and must not exceed 2000 characters")
            );
        }

        return null;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequest request,
            Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }

            User currentUser = currentUserOpt.get();

            boolean isAdmin = rbacService.validateRoles(authentication, "ROLE_ADMIN");

            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Post not found"));
            }

            Resource resource = resourceOpt.get();

            if (!abacService.canEdit(currentUser, resource, isAdmin)) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id, Authentication authentication) {
        try {


            Long currentUserId = Long.parseLong(authentication.getName());
            Optional<User> currentUserOpt = userRepository.findById(currentUserId);
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found"));
            }

            User currentUser = currentUserOpt.get();
            boolean isAdmin = rbacService.validateRoles(authentication, "ROLE_ADMIN");
            if(!isAdmin){
                abacService.evaluateDeletePolicy();
            }

            boolean isMod = rbacService.validateRoles(authentication, "ROLE_MOD");
            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Post not found"));
            }
            Resource resource = resourceOpt.get();

            if (abacService.canDelete(currentUser, resource, isAdmin, isMod)) {
                resourceRepository.delete(resource);
                return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No permission to delete this post"));
            }

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() != null ? e.getReason() : e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }
}