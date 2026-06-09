package org.example.backend.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklistService {

    // Mapa przechowująca token jako klucz i jego datę wygaśnięcia jako wartość
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expirationDate) {
        blacklist.put(token, expirationDate);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    // Ta metoda uruchamia się automatycznie co 1 godzinę
    @Scheduled(fixedRate = 3600000)
    public void cleanUpBlacklist() {
        Date now = new Date();

        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
        System.out.println("Wyczyszczono przedawnione tokeny z czarnej listy.");
    }


}