package com.expensemanager.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                buildCache("dashboardStats", 5, TimeUnit.MINUTES, 100),
                buildCache("categories", 60, TimeUnit.MINUTES, 50),
                buildCache("managers", 30, TimeUnit.MINUTES, 100),
                buildCache("userExpenseSummary", 10, TimeUnit.MINUTES, 500)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, int maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(duration, unit)
                        .maximumSize(maxSize)
                        .build());
    }
}
