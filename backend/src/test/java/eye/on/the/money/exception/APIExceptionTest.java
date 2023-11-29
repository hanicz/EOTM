package eye.on.the.money.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class APIExceptionTest {

    @Test
    public void APIException() {
        APIException apiException = new APIException("MSG");

        Assertions.assertEquals("MSG", apiException.getMessage());
    }
}