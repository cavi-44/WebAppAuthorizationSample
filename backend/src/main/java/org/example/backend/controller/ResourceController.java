package org.example.backend.controller;

import org.example.backend.model.Resource;
import org.example.backend.model.User;
import org.example.backend.repository.ResourceRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ResourceController(ResourceRepository resourceRepository, UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
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
            Long teamId = currentUser.getTeam() != null ? currentUser.getTeam().getId() : null;
            String currentUserRole = currentUser.getRole().getName();
            boolean isAdmin = currentUserRole.equals("ROLE_ADMIN");

            // max 15 posts per request
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

                Optional<User> authorOpt = userRepository.findById(resource.getAuthorId());
                if (authorOpt.isPresent()) {
                    User author = authorOpt.get();
                    dto.authorLogin = author.getLogin();
                    dto.authorTeamName = author.getTeam() != null ? author.getTeam().getName() : "No Team";
                } else {
                    dto.authorLogin = "Deleted User";
                    dto.authorTeamName = "No Team";
                }

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

    @PostMapping
    public ResponseEntity<?> createResource(@RequestBody ResourceRequest request, Authentication authentication) {
        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            
            if (request.title == null || request.title.trim().isEmpty() ||
                request.description == null || request.description.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Title and description are required"));
            }

            Resource resource = new Resource();
            resource.setTitle(request.title);
            resource.setDescription(request.description);
            resource.setAuthorId(currentUserId);
            resource.setPrivate(request.isPrivate != null && request.isPrivate);

            resourceRepository.save(resource);
            return ResponseEntity.ok(Map.of("message", "Created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Error creating post"));
        }
    }

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

            // only author or admin can edit
            if (!isAuthor && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No permission to edit this post"));
            }

            if (request.title != null && !request.title.trim().isEmpty()) {
                resource.setTitle(request.title);
            }
            if (request.description != null && !request.description.trim().isEmpty()) {
                resource.setDescription(request.description);
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
            String currentUserRole = currentUser.getRole().getName();
            boolean isAdmin = currentUserRole.equals("ROLE_ADMIN");

            Optional<Resource> resourceOpt = resourceRepository.findById(id);
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Post not found"));
            }

            Resource resource = resourceOpt.get();
            boolean isAuthor = resource.getAuthorId().equals(currentUserId);

            // check if mod of the same team
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
