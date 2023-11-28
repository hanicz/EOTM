package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.news.News;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class NewsAPIServiceImplTest {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private NewsAPIServiceImpl newsAPIService;
    private MockRestServiceServer mockServer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeEach
    public void init() {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @Test
    public void getNews() throws URISyntaxException, JsonProcessingException {
        List<News> news = this.createNewsList();

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://finnhubhost.com/news?category=cat&token=token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(news)));

        List<News> response = this.newsAPIService.getNews("cat");
        this.mockServer.verify();
        Assertions.assertEquals(news, response);
    }

    @Test
    public void getCompanyNews() throws URISyntaxException, JsonProcessingException {
        List<News> news = this.createNewsList();

        Date threeMonths = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        String fromDate = this.dateFormat.format(threeMonths);
        String toDate = this.dateFormat.format(new Date());

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI(MessageFormat.format("https://finnhubhost.com/company-news?symbol=symbol&from={0}&to={1}&token=token", fromDate, toDate))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(news)));

        List<News> response = this.newsAPIService.getCompanyNews("symbol");
        this.mockServer.verify();
        Assertions.assertEquals(news, response);
    }

    @Test
    public void clientException() throws URISyntaxException {
        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://finnhubhost.com/news?category=cat&token=token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.newsAPIService.getNews("cat");
        });

        this.mockServer.verify();

        String expectedMessage = "Unable to reach news API";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void nullBody() throws URISyntaxException {
        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://finnhubhost.com/news?category=cat&token=token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.newsAPIService.getNews("cat");
        });

        this.mockServer.verify();

        String expectedMessage = "Empty response from news API";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void wrongJSONObject() throws URISyntaxException {
        String json = "{\"json\":\"json\"}";

        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://finnhubhost.com/news?category=cat&token=token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.newsAPIService.getNews("cat");
        });

        this.mockServer.verify();

        String expectedMessage = "Unable to reach news API";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void invalidJSONObject() throws URISyntaxException {
        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://finnhubhost.com/news?category=cat&token=token")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("NOT JSON"));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.newsAPIService.getNews("cat");
        });

        this.mockServer.verify();

        String expectedMessage = "Unable to reach news API";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    private List<News> createNewsList() {
        List<News> news = new ArrayList<>();
        news.add(News.builder().id(1L).url("URL").image("img").category("cat").source("src").summary("sum").headline("headline").datetime(new Date().getTime()).build());
        news.add(News.builder().id(2L).url("URL").image("img").category("cat3").source("src").summary("sum2").headline("headlin2e").datetime(new Date().getTime()).build());
        news.add(News.builder().id(3L).url("URL").image("img").category("cat2").source("src").summary("sum3").headline("headline3").datetime(new Date().getTime()).build());

        return news;
    }
}