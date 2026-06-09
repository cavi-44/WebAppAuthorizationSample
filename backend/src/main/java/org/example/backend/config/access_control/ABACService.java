package org.example.backend.config.access_control;

import org.example.backend.model.Resource;
import org.example.backend.model.User;
import org.example.backend.repository.ResourceRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ABACService {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;



    public void evaluateDeletePolicy() {
        LocalTime now = LocalTime.now();

        if (now.isAfter(LocalTime.of(14, 0)) || now.isBefore(LocalTime.of(8, 0))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Destructive actions (DELETE) are blocked between 22:00 and 08:00");
        }
    }
    public ABACService(UserRepository userRepository, ResourceRepository resourceRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
    }



    public Resource getVisibleResourceOrThrow(Long resourceId, User currentUser, boolean isAdmin) {
        List<Resource> visibleResources = this.getVisibleResources(currentUser, isAdmin, 0, 100);


        return visibleResources.stream()
                .filter(r -> r.getId().equals(resourceId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }
    public List<Resource> getVisibleResources(User currentUser, boolean isAdmin, int page, int pageSize) {
        Long currentUserId = currentUser.getId();
        Long teamId = currentUser.getTeam() != null ? currentUser.getTeam().getId() : null;

        int finalPageSize = Math.min(pageSize, 15);


        return resourceRepository.findVisibleResources(
                currentUserId,
                teamId,
                isAdmin,
                PageRequest.of(page, finalPageSize)
        );
    }
    public boolean isAuthor(User currentUser, Resource resource) {
        if (currentUser == null || resource == null) {
            return false;
        }
        return resource.getAuthorId().equals(currentUser.getId());
    }
    public boolean canEdit(User currentUser, Resource resource, boolean isAdmin) {
        return isAdmin || isAuthor(currentUser, resource);
    }
    public boolean canDelete(User currentUser, Resource resource, boolean isAdmin, boolean isMod) {

        if (canEdit(currentUser, resource, isAdmin)) {
            return true;
        }

        if (isMod) {
            Optional<User> authorOpt = userRepository.findById(resource.getAuthorId());
            if (authorOpt.isPresent()) {
                User author = authorOpt.get();
                if (author.getTeam() != null && currentUser.getTeam() != null) {
                    return author.getTeam().getId().equals(currentUser.getTeam().getId());
                }
            }
        }

        return false;
    }
}