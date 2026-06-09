package org.example.backend.config.access_control;

import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
public class RBACService {

    private final UserRepository userRepository;


    public RBACService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean validateRoles(Authentication authentication, String... allowedRoles) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }


        //WERSJA Z JWT
//        return authentication.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .anyMatch(userRole -> // Bezpieczne porównanie: akceptuje formaty z prefiksem "ROLE_" lub bez niego
//                        Arrays.asList(allowedRoles).contains(userRole));





        Long currentUserId = Long.parseLong(authentication.getName());
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in database"));

        String userRole = currentUser.getRole().getName();


        return Arrays.asList(allowedRoles).contains(userRole);

    }
}