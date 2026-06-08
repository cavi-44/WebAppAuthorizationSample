package org.example.backend.controller;

import org.example.backend.model.Role;
import org.example.backend.model.Team;
import org.example.backend.model.User;
import org.example.backend.repository.RoleRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.teamRepository = teamRepository;
    }

    // GET all users (everyone logged in can view)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // GET user details by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        return ResponseEntity.ok(userOpt.get());
    }

    // SET user role (ADMIN only)
    @PutMapping("/{id}/role")
    public ResponseEntity<?> setUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only administrator can change roles"));
        }

        String roleName = payload.get("role");
        if (roleName == null || roleName.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Role is required"));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        Optional<Role> roleOpt = roleRepository.findById(roleName);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Role does not exist"));
        }

        User user = userOpt.get();
        user.setRole(roleOpt.get());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    // EDIT team, dodanie uzytkownika (Self or ADMIN)
    @PutMapping("/{id}/team")
    public ResponseEntity<?> setUserTeam(
            @PathVariable Long id,
            @RequestBody Map<String, Long> payload,
            Authentication authentication) {

        Long currentUserId = Long.parseLong(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You cannot change team of other users"));
        }

        Long teamId = payload.get("teamId");
        if (teamId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Team ID is required"));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Team does not exist"));
        }

        User user = userOpt.get();
        user.setTeam(teamOpt.get());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Team updated successfully"));
    }
}
