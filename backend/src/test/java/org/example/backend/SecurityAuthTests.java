package org.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuthTests {

    @Autowired
    private MockMvc mockMvc;

    private String login(String username, String password) throws Exception {
        String json = String.format("{\"login\":\"%s\", \"password\":\"%s\"}", username, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // Extract JWT token from JSON response {"token":"..."}
        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    public void testUnauthorizedAccessFails() throws Exception {
        // Accessing resources without a token should fail with 403 (Forbidden) in Spring Security config
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSwaggerUiAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    public void testRolesAccessControl() throws Exception {
        String userToken = login("real_user", "user123");
        String adminToken = login("admin", "admin123");

        // 1. Changing roles is restricted to ADMIN. A regular user should get 403 Forbidden.
        mockMvc.perform(put("/api/users/2/role")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden());

        // 2. ADMIN should successfully change a role.
        mockMvc.perform(put("/api/users/2/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testTeamAccessIsolation() throws Exception {
        String realUserToken = login("real_user", "user123");
        String barcaUserToken = login("barca_user", "user123");

        // 1. real_user (Team: Real Madrid) requests posts.
        // They should see: Real Madrid public & private, FC Barcelona public.
        // They should NOT see FC Barcelona private posts.
        String realFeed = mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + realUserToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(realFeed.contains("Real Madrid Secret Tactics"), "Should see Real Madrid private post");
        org.junit.jupiter.api.Assertions.assertTrue(realFeed.contains("Barcelona stadium renovation"), "Should see Barcelona public post");
        org.junit.jupiter.api.Assertions.assertFalse(realFeed.contains("FC Barcelona board updates"), "Should NOT see Barcelona private post");

        // 2. barca_user (Team: FC Barcelona) requests posts.
        // They should see: FC Barcelona public & private, Real Madrid public.
        // They should NOT see Real Madrid private posts.
        String barcaFeed = mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + barcaUserToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(barcaFeed.contains("FC Barcelona board updates"), "Should see Barcelona private post");
        org.junit.jupiter.api.Assertions.assertTrue(barcaFeed.contains("Real Madrid in Champions League"), "Should see Real Madrid public post");
        org.junit.jupiter.api.Assertions.assertFalse(barcaFeed.contains("Real Madrid Secret Tactics"), "Should NOT see Real Madrid private post");
    }

    @Test
    public void testModeratorDeletePermissions() throws Exception {
        String realUserToken = login("real_user", "user123");
        String barcaModToken = login("barca_mod", "mod123");

        // 1. Create a private post as real_user (Team: Real Madrid).
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + realUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Real Private Test\",\"description\":\"Test description\",\"isPrivate\":true}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 2. Get resources as real_user to find the created post's ID.
        String feed = mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + realUserToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Simple extraction of ID
        String marker = "\"title\":\"Real Private Test\"";
        int index = feed.indexOf(marker);
        org.junit.jupiter.api.Assertions.assertTrue(index != -1);
        // Find the "id": field before the title
        int idStart = feed.lastIndexOf("\"id\":", index) + 5;
        int idEnd = feed.indexOf(",", idStart);
        String idStr = feed.substring(idStart, idEnd).trim();
        Long postId = Long.parseLong(idStr);

        // 3. barca_mod (Team: FC Barcelona) tries to delete this post.
        // They should fail with 403 Forbidden because they are a moderator of a DIFFERENT team.
        mockMvc.perform(delete("/api/resources/" + postId)
                        .header("Authorization", "Bearer " + barcaModToken))
                .andExpect(status().isForbidden());

        // 4. real_mod (Team: Real Madrid) tries to delete this post.
        // They should succeed because they are a moderator of the SAME team.
        String realModToken = login("real_mod", "mod123");
        mockMvc.perform(delete("/api/resources/" + postId)
                        .header("Authorization", "Bearer " + realModToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testModeratorCannotEdit() throws Exception {
        String realUserToken = login("real_user", "user123");
        String realModToken = login("real_mod", "mod123");

        // 1. Create a post as real_user
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + realUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Real Edit Test\",\"description\":\"Test description\",\"isPrivate\":true}"))
                .andExpect(status().isOk());

        // 2. Get feed to extract post ID
        String feed = mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + realUserToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int index = feed.indexOf("\"title\":\"Real Edit Test\"");
        int idStart = feed.lastIndexOf("\"id\":", index) + 5;
        int idEnd = feed.indexOf(",", idStart);
        Long postId = Long.parseLong(feed.substring(idStart, idEnd).trim());

        // 3. Try to edit as real_mod (moderator of the same team, but not author).
        // It should fail (403 Forbidden) because mods can delete, but CANNOT edit.
        mockMvc.perform(put("/api/resources/" + postId)
                        .header("Authorization", "Bearer " + realModToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Edited by Mod\",\"description\":\"Should fail\"}"))
                .andExpect(status().isForbidden());

        // 4. Try to edit as author (real_user). Should succeed.
        mockMvc.perform(put("/api/resources/" + postId)
                        .header("Authorization", "Bearer " + realUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Edited by Author\",\"description\":\"Should succeed\"}"))
                .andExpect(status().isOk());
    }
}
