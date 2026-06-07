package org.example.backend.repository;

import org.example.backend.model.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE " +
           ":isAdmin = true OR " +
           "r.isPrivate = false OR " +
           "r.authorId = :userId OR " +
           "(:teamId IS NOT NULL AND r.isPrivate = true AND r.authorId IN (SELECT u.id FROM User u WHERE u.team.id = :teamId)) " +
           "ORDER BY r.creationDate DESC")
    List<Resource> findVisibleResources(
            @Param("userId") Long userId,
            @Param("teamId") Long teamId,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable
    );
}
