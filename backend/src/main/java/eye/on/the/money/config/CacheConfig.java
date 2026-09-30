package eye.on.the.money.config;

import eye.on.the.money.service.financial.BankReportCaches;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new EvictingCacheErrorHandler();
    }

    @Bean
    @Profile("!test")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .transactionAware()
                .withCacheConfiguration(BankReportCaches.MONTHLY,
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7L)))
                .withCacheConfiguration(BankReportCaches.INCOME,
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7L)))
                .withCacheConfiguration(BankReportCaches.CATEGORY,
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7L)))
                .withCacheConfiguration(BankReportCaches.YEARLY,
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7L)))
                .withCacheConfiguration("exchanges",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(3L)))
                .withCacheConfiguration("symbols",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(3L)))
                .withCacheConfiguration("token",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(23L)))
                .withCacheConfiguration("holdings-stock",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("holdings-crypto",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("holdings-etf",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("holdings-forex",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("grants-rsu",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("grants-star",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("rates",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(8L)))
                .withCacheConfiguration("news-category",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1L)))
                .withCacheConfiguration("news-company",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1L)))
                .withCacheConfiguration("news-portfolio",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1L)))
                .withCacheConfiguration("news-reddit",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1L)));
    }
}
