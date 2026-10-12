package com.retrofit.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimiterService {

    @Value("${ratelimit.auth.capacity:5}")
    private long authCapacity;

    @Value("${ratelimit.auth.refill-duration-seconds:60}")
    private long authRefillDurationSeconds;

    @Value("${ratelimit.api.capacity:100}")
    private long apiCapacity;

    @Value("${ratelimit.api.refill-duration-seconds:60}")
    private long apiRefillDurationSeconds;

    private static class BucketEntry {
        final Bucket bucket;
        volatile Instant lastAccess;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        void touch() {
            this.lastAccess = Instant.now();
        }
    }

    private final Map<String, BucketEntry> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> apiBuckets = new ConcurrentHashMap<>();

    public ConsumptionProbe tryConsumeAuth(String ip) {
        BucketEntry entry = authBuckets.computeIfAbsent(ip, k -> new BucketEntry(createAuthBucket()));
        entry.touch();
        return entry.bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeApi(String ip) {
        BucketEntry entry = apiBuckets.computeIfAbsent(ip, k -> new BucketEntry(createApiBucket()));
        entry.touch();
        return entry.bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(authCapacity)
                .refillGreedy(authCapacity, Duration.ofSeconds(authRefillDurationSeconds))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(apiCapacity)
                .refillGreedy(apiCapacity, Duration.ofSeconds(apiRefillDurationSeconds))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Limpieza periódica cada 10 minutos para liberar memoria de IPs inactivas (anti-memory leak)
    @Scheduled(fixedRate = 600000)
    public void cleanupStaleBuckets() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(15));
        int initialAuth = authBuckets.size();
        int initialApi = apiBuckets.size();
        authBuckets.entrySet().removeIf(e -> e.getValue().lastAccess.isBefore(threshold));
        apiBuckets.entrySet().removeIf(e -> e.getValue().lastAccess.isBefore(threshold));
        log.debug("RateLimiter cleanup: auth {} -> {}, api {} -> {}", initialAuth, authBuckets.size(), initialApi, apiBuckets.size());
    }
}
