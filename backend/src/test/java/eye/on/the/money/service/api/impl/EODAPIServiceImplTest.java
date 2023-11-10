package eye.on.the.money.service.api.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class EODAPIServiceImplTest {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private EODAPIServiceImpl eodAPIService;

    private MockRestServiceServer mockServer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void init() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void getLiveValue() throws URISyntaxException, JsonProcessingException {
        String json = "{\"json\":\"json\"}";
        this.mockServer.expect(ExpectedCount.once(),
                requestTo(new URI("https://eodhost.com/real-time/stock/?api_token=token&fmt=json&s=AMD.US")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json));

        JsonNode response = this.eodAPIService.getLiveValue("AMD.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        this.mockServer.verify();
        Assertions.assertEquals(json, response.toString());
    }

}
