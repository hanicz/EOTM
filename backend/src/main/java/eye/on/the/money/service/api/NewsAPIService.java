package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.util.DateFormats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class NewsAPIService extends APIService {

    private final static String API = "finnhub";

    private final static String NEWS_PATH = "/news?category={1}&token={0}";
    private final static String COMPANY_NEWS_PATH = "/company-news?symbol={1}&from={2}&to={3}&token={0}";

    @Autowired
    public NewsAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                          WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<News> getNews(String category) {
        log.trace("Enter");
        String url = this.createURL(NewsAPIService.API, NEWS_PATH, category);
        ResponseEntity<?> response = this.callGetAPI(url, News[].class);
        return Arrays.asList((News[]) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<News> getCompanyNews(String symbol) {
        log.trace("Enter getCompanyNews");
        String fromDate = LocalDate.now().minusDays(30).format(DateFormats.YYYY_MM_DD);
        String toDate = LocalDate.now().format(DateFormats.YYYY_MM_DD);
        String url = this.createURL(NewsAPIService.API, COMPANY_NEWS_PATH, symbol, fromDate, toDate);
        ResponseEntity<?> response = this.callGetAPI(url, News[].class);
        return Arrays.asList((News[]) response.getBody());
    }
}
