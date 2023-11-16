package eye.on.the.money.alert;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.mail.EmailService;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class AlertSchedulerTest {

    @MockBean
    private EODAPIService eodapiService;

    @MockBean
    private EmailService emailService;

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private AlertScheduler alertScheduler;


}