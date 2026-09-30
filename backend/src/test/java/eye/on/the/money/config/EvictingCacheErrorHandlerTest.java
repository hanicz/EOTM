package eye.on.the.money.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.SerializationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EvictingCacheErrorHandlerTest {

    private static final Long KEY = 42L;

    private final EvictingCacheErrorHandler handler = new EvictingCacheErrorHandler();

    @Mock
    private Cache cache;

    @Test
    void getErrorEvictsKeyAndDoesNotThrow() {
        assertDoesNotThrow(() -> handler.handleCacheGetError(new SerializationException("bad class"), cache, KEY));

        verify(cache).evict(KEY);
    }

    @Test
    void getErrorDoesNotThrowWhenEvictFails() {
        doThrow(new RedisConnectionFailureException("redis down")).when(cache).evict(KEY);

        assertDoesNotThrow(() -> handler.handleCacheGetError(new SerializationException("bad class"), cache, KEY));
    }

    @Test
    void putErrorIsRethrown() {
        RuntimeException exception = new IllegalStateException("redis down");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> handler.handleCachePutError(exception, cache, KEY, "value"));

        assertSame(exception, thrown);
    }
}
