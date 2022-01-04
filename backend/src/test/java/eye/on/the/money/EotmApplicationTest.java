package eye.on.the.money;

import eye.on.the.money.controller.DividendController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EotmApplicationTest {

    @Autowired
    private DividendController dividendController;

    @Test
    public void contextLoads() {
        assertThat(this.dividendController).isNotNull();
    }
}