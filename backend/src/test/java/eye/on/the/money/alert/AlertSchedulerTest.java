package eye.on.the.money.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.service.mail.EmailService;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AlertScheduler alertScheduler;

    private final ObjectMapper om = new ObjectMapper();

    @AfterEach
    public void cleanUpEach(){
        this.stockAlertRepository.deleteAll();
    }

    @Test
    public void checkAlertsPriceOver() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PRICE_OVER", 50.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.01,\"change_p\":0.0086}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(1)).sendMail("test@test.test", "CRSR.US is over 50.0 price point");
        Assertions.assertFalse(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPriceOverNoTrigger() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PRICE_OVER", 50.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":49.99,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.01,\"change_p\":0.0086}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(0)).sendMail(anyString(), anyString());
        Assertions.assertTrue(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPriceUnder() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PRICE_UNDER", 200.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.01,\"change_p\":0.0086}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(1)).sendMail("test@test.test", "CRSR.US is under 200.0 price point");
        Assertions.assertFalse(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPriceUnderNoTrigger() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PRICE_UNDER", 200.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":200.01,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.01,\"change_p\":0.0086}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(0)).sendMail(anyString(), anyString());
        Assertions.assertTrue(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPercentOver() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PERCENT_OVER", 5.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.1,\"change_p\":6.78}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(1)).sendMail("test@test.test", "CRSR.US is over 5.0%");
        Assertions.assertFalse(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPercentOverNoTrigger() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PERCENT_OVER", 5.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":0.1,\"change_p\":4.99}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(0)).sendMail(anyString(), anyString());
        Assertions.assertTrue(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPercentUnder() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PERCENT_UNDER", -5.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":-0.06,\"change_p\":-6.89}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(1)).sendMail("test@test.test", "CRSR.US is under -5.0%");
        Assertions.assertFalse(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsPercentUnderNoTrigger() throws JsonProcessingException {
        StockAlert sa = this.createNewAlert("PERCENT_UNDER", -5.0);
        sa = this.stockAlertRepository.save(sa);

        JsonNode json = om.readTree("[{\"code\":\"CRSR.US\",\"timestamp\":1700859720,\"gmtoffset\":0,\"open\":116.49,\"high\":116.59,\"low\":115.34,\"close\":116.25,\"volume\":2149550,\"previousClose\":116.24,\"change\":-0.06,\"change_p\":-4.99}]");

        doNothing().when(this.emailService).sendMail(anyString(), anyString());
        when(this.eodapiService.getLiveValue("CRSR.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}")).thenReturn(json);

        this.alertScheduler.checkAlerts();

        verify(this.emailService, times(0)).sendMail(anyString(), anyString());
        Assertions.assertTrue(this.stockAlertRepository.existsById(sa.getId()));
    }

    @Test
    public void checkAlertsNoAlert() throws JsonProcessingException {
        this.alertScheduler.checkAlerts();

        verify(this.eodapiService, times(0)).getLiveValue(anyString(), anyString());
    }

    private StockAlert createNewAlert(String type, Double vp) {
        User user = this.userRepository.findByEmail("test@test.test");
        Stock stock = this.stockRepository.findById("crsr").get();

        return StockAlert.builder().stock(stock).user(user).valuePoint(vp).type(type).build();
    }
}