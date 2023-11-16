package eye.on.the.money.exception;

import eye.on.the.money.EotmApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class APIExceptionTest {

    @Test
    public void APIException() {
        APIException apiException = new APIException("MSG");

        Assertions.assertEquals("MSG", apiException.getMessage());
    }
}