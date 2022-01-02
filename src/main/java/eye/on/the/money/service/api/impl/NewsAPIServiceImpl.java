package eye.on.the.money.service.api.impl;

import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.NewsAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NewsAPIServiceImpl implements NewsAPIService {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public List<News> getNews(String category) {
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(stockAPI + "/news?category={0}&token={1}", category, secret);
        return this.callNewsAPI(URL);
    }

    @Override
    public List<News> getCompanyNews(String symbol) {
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        Date threeMonths = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        String URL =
                MessageFormat.format(stockAPI + "/company-news?symbol={0}&from={1}&to={2}&token={3}",
                        symbol,
                        NewsAPIServiceImpl.dateFormat.format(threeMonths),
                        NewsAPIServiceImpl.dateFormat.format(new Date()),
                        secret);
        return this.callNewsAPI(URL);
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private List<News> callNewsAPI(String URL) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<News[]> response = restTemplate.getForEntity(URL, News[].class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return Arrays.asList(response.getBody());
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }
}
