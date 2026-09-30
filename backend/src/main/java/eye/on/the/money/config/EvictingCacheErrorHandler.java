package eye.on.the.money.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.dao.DataAccessException;

@Slf4j
public class EvictingCacheErrorHandler extends SimpleCacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Discarding unreadable cache entry {}::{}: {}", cache.getName(), key, exception.getMessage());
        try {
            cache.evict(key);
        } catch (DataAccessException evictException) {
            log.warn("Could not evict cache entry {}::{}: {}", cache.getName(), key, evictException.getMessage());
        }
    }
}
