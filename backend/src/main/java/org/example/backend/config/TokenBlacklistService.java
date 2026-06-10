package org.example.backend.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklistService {

    // key:token; value: expiration_date
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expirationDate) {
        blacklist.put(token, expirationDate);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    // automatically each hour
    @Scheduled(fixedRate = 3600000)
    public void cleanUpBlacklist() {
        Date now = new Date();

        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
        System.out.println("Wyczyszczono przedawnione tokeny z czarnej listy.");
    }
}