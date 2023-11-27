package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = EotmApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getAlerts() throws Exception {
    }
}