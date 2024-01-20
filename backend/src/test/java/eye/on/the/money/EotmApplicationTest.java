package eye.on.the.money;

import eye.on.the.money.controller.DividendController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class EotmApplicationTest {

    @Autowired
    private DividendController dividendController;

    @Test
    public void contextLoads() {
        assertThat(this.dividendController).isNotNull();
    }
}