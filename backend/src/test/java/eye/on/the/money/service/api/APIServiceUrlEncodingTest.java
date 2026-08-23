package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.model.Config;
import eye.on.the.money.model.Credential;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class APIServiceUrlEncodingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<URI> captured = new AtomicReference<>();

    private WebClient capturingClient(String body) {
        return WebClient.builder()
                .exchangeFunction(request -> {
                    this.captured.set(request.url());
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body(body)
                            .build());
                })
                .build();
    }

    private ConfigRepository configRepository(String api, String url) {
        ConfigRepository configRepository = mock(ConfigRepository.class);
        when(configRepository.findById(api)).thenReturn(Optional.of(new Config(api, url)));
        return configRepository;
    }

    private CredentialRepository credentialRepository(String api) {
        CredentialRepository credentialRepository = mock(CredentialRepository.class);
        when(credentialRepository.findById(api)).thenReturn(Optional.of(new Credential(api, "secretToken")));
        return credentialRepository;
    }

    @Test
    void queryParameterInjectionIsEncoded() {
        StockMetricAPIService service = new StockMetricAPIService(
                this.credentialRepository("finnhub"),
                this.configRepository("finnhub", "https://finnhub.io/api/v1"),
                this.capturingClient("{\"metric\":{}}"), this.objectMapper);

        service.getMetric("AAPL&token=attacker&x=");

        URI uri = this.captured.get();
        Assertions.assertEquals("finnhub.io", uri.getHost());
        Assertions.assertTrue(uri.getRawQuery().contains("symbol=AAPL%26token%3Dattacker%26x%3D"),
                "injected separators must be encoded, was: " + uri.getRawQuery());
        Assertions.assertTrue(uri.getRawQuery().endsWith("token=secretToken"),
                "api token must remain the last parameter, was: " + uri.getRawQuery());
    }

    @Test
    void pathTraversalIsEncoded() {
        EODAPIService service = new EODAPIService(
                this.credentialRepository("eod"),
                this.configRepository("eod", "https://eodhd.com/api"),
                this.capturingClient("[]"), this.objectMapper);

        service.getAllSymbols("../../../admin");

        URI uri = this.captured.get();
        Assertions.assertEquals("eodhd.com", uri.getHost());
        Assertions.assertEquals("/api/exchange-symbol-list/..%2F..%2F..%2Fadmin", uri.getRawPath(),
                "separators in a path segment must be encoded");
    }

    @Test
    void braceInInputDoesNotBreakTemplating() {
        StockMetricAPIService service = new StockMetricAPIService(
                this.credentialRepository("finnhub"),
                this.configRepository("finnhub", "https://finnhub.io/api/v1"),
                this.capturingClient("{\"metric\":{}}"), this.objectMapper);

        service.getMetric("{0}{1}'quoted'");

        URI uri = this.captured.get();
        Assertions.assertTrue(uri.getRawQuery().endsWith("token=secretToken"),
                "was: " + uri.getRawQuery());
        Assertions.assertFalse(uri.getRawQuery().contains("secretToken&"),
                "secret must not be substituted into the symbol, was: " + uri.getRawQuery());
    }

    @Test
    void ordinarySymbolIsUnchanged() {
        StockMetricAPIService service = new StockMetricAPIService(
                this.credentialRepository("finnhub"),
                this.configRepository("finnhub", "https://finnhub.io/api/v1"),
                this.capturingClient("{\"metric\":{}}"), this.objectMapper);

        service.getMetric("AAPL");

        Assertions.assertTrue(this.captured.get().getRawQuery().contains("symbol=AAPL&"),
                "was: " + this.captured.get().getRawQuery());
    }

    @Test
    void tickerListSeparatorsSurviveEncoding() {
        EODAPIService service = new EODAPIService(
                this.credentialRepository("eod"),
                this.configRepository("eod", "https://eodhd.com/api"),
                this.capturingClient("[]"), this.objectMapper);

        service.getLiveStockValue("AAPL.US,MSFT.US");

        Assertions.assertTrue(this.captured.get().getRawQuery().contains("s=AAPL.US,MSFT.US"),
                "was: " + this.captured.get().getRawQuery());
    }
}
