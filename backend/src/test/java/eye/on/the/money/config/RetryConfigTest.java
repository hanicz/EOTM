package eye.on.the.money.config;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.exception.APIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(classes = {EotmApplication.class, RetryConfigTest.RetryTestConfig.class})
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class RetryConfigTest {

    static class AlwaysFailing {
        private final AtomicInteger attempts = new AtomicInteger();

        @Retryable(retryFor = APIException.class, maxAttempts = 3)
        public void call() {
            this.attempts.incrementAndGet();
            throw new APIException("always fails");
        }

        public int getAttempts() {
            return this.attempts.get();
        }
    }

    @TestConfiguration
    static class RetryTestConfig {
        @Bean
        public AlwaysFailing alwaysFailing() {
            return new AlwaysFailing();
        }
    }

    @Autowired
    private AlwaysFailing alwaysFailing;

    @Test
    void retryableMethodsAreProxiedAndRetried() {
        Assertions.assertThrows(APIException.class, () -> this.alwaysFailing.call());
        Assertions.assertEquals(3, this.alwaysFailing.getAttempts());
    }
}
