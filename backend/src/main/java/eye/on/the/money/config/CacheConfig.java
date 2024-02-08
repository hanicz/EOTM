package eye.on.the.money.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!test")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("exchanges",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(3L)))
                .withCacheConfiguration("symbols",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(3L)))
                .withCacheConfiguration("token",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5L)));
    }
}
