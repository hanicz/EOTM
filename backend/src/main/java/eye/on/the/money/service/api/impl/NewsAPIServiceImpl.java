package eye.on.the.money.service.api.impl;

import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.NewsAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class NewsAPIServiceImpl extends APIService implements NewsAPIService {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static String API = "finnhub";

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<News> getNews(String category) {
        log.trace("Enter");
        return this.callNewsAPI(this.createURL(NewsAPIServiceImpl.API, "/news?category={1}&token={0}", category));
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<News> getCompanyNews(String symbol) {
        log.trace("Enter getCompanyNews");
        Date threeMonths = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        String fromDate = this.dateFormat.format(threeMonths);
        String toDate = this.dateFormat.format(new Date());

        return this.callNewsAPI(this.createURL(NewsAPIServiceImpl.API, "/company-news?symbol={1}&from={2}&to={3}&token={0}", symbol, fromDate, toDate));
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private List<News> callNewsAPI(String URL) {
        log.trace("Enter callNewsAPI");
        try {
            ResponseEntity<News[]> response = this.restTemplate.getForEntity(URL, News[].class);
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                log.error("Empty response from news API");
                throw new APIException("Empty response from news API");
            }
        } catch (RestClientException e) {
            log.error("Unable to reach news API: " + e.getMessage());
            throw new APIException("Unable to reach news API");
        }
    }
}
