package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.EtfProfileDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtfProfileAPIServiceTest {

    private static final String API = "etf";

    private static final String FULL_PAGE = """
            <html><body>
            <span data-testid="etf-profile-header_isin-value">XX0000000017</span>
            <div data-testid="etf-profile-header_ter-value">0.12% p.a.</div>
            <div data-testid="etf-profile-header_distribution-policy-value">Accumulating</div>
            <div data-testid="etf-profile-header_replication-value">Physical</div>
            <div data-testid="etf-profile-header_inception-date-value">1 March 2020</div>
            <div data-testid="etf-profile-header_holdings-value">1,234</div>
            <div data-testid="etf-profile-header_fund-size-value-wrapper"> <span> EUR 1,500 </span> m </div>
            <table>
            <tr><td><div data-testid="tl_etf-basics_value_index-name">Sample Global Index</div></td></tr>
            <tr><td><div data-testid="tl_etf-basics_value_investment-focus">Equity, World</div></td></tr>
            <tr><td><div data-testid="tl_etf-basics_value_fund-currency">USD</div></td></tr>
            <tr><td><div data-testid="tl_etf-basics_value_currency-hedge">Currency unhedged</div></td></tr>
            <tr><td><div data-testid="tl_etf-basics_value_volatility">14.20%</div></td></tr>
            <tr><td><div data-testid="tl_etf-basics_value_fund-provider">Sample Funds</div></td></tr>
            </table>
            <table data-testid="etf-holdings_top-holdings_table">
            <tr data-testid="etf-holdings_top-holdings_row">
              <td><a data-testid="tl_etf-holdings_top-holdings_link_name" href="/en/stock-profiles/XX1111111111" title="Alpha Corp">Alpha Corp</a></td>
              <td><span data-testid="tl_etf-holdings_top-holdings_value_percentage">6.50%</span></td>
            </tr>
            <tr data-testid="etf-holdings_top-holdings_row">
              <td><a data-testid="tl_etf-holdings_top-holdings_link_name" href="/en/stock-profiles/XX2222222222" title="Beta Inc">Beta</a></td>
              <td><span data-testid="tl_etf-holdings_top-holdings_value_percentage">3.25%</span></td>
            </tr>
            </table>
            <table data-testid="etf-holdings_countries_table">
            <tr><td data-testid="tl_etf-holdings_countries_value_name">Atlantis</td><td data-testid="tl_etf-holdings_countries_value_percentage">70.00%</td></tr>
            <tr><td data-testid="tl_etf-holdings_countries_value_name">Other</td><td data-testid="tl_etf-holdings_countries_value_percentage">30.00%</td></tr>
            </table>
            <table data-testid="etf-holdings_sectors_table">
            <tr><td data-testid="tl_etf-holdings_sectors_value_name">Technology</td><td data-testid="tl_etf-holdings_sectors_value_percentage">55.50%</td></tr>
            </table>
            </body></html>
            """;

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://etfhost.com/en")));
    }

    private EtfProfileAPIService serviceWithResponse(int statusCode, String body, AtomicReference<ClientRequest> captured) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    captured.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.valueOf(statusCode))
                            .header("Content-Type", MediaType.TEXT_HTML_VALUE)
                            .body(body)
                            .build());
                })
                .build();
        return new EtfProfileAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getProfile_parsesFullPage() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        EtfProfileAPIService service = this.serviceWithResponse(200, FULL_PAGE, captured);

        EtfProfileDTO result = service.getProfile("XX0000000017").orElseThrow();

        assertEquals("https://etfhost.com/en/etf-profile.html?isin=XX0000000017", captured.get().url().toString());
        assertNotNull(captured.get().headers().getFirst(HttpHeaders.USER_AGENT));
        assertEquals("XX0000000017", result.getIsin());
        assertEquals("Sample Funds", result.getProvider());
        assertEquals("Sample Global Index", result.getIndexName());
        assertEquals("Equity, World", result.getInvestmentFocus());
        assertEquals("EUR 1,500 m", result.getFundSize());
        assertEquals("0.12% p.a.", result.getTer());
        assertEquals("Physical", result.getReplication());
        assertEquals("Accumulating", result.getDistributionPolicy());
        assertEquals("USD", result.getFundCurrency());
        assertEquals("Currency unhedged", result.getCurrencyHedge());
        assertEquals("1 March 2020", result.getInceptionDate());
        assertEquals("14.20%", result.getVolatility());
        assertEquals(1234, result.getTotalHoldings());

        assertEquals(2, result.getHoldings().size());
        assertEquals("Alpha Corp", result.getHoldings().get(0).getName());
        assertEquals("XX1111111111", result.getHoldings().get(0).getIsin());
        assertEquals(new BigDecimal("6.50"), result.getHoldings().get(0).getWeight());
        assertEquals("Beta Inc", result.getHoldings().get(1).getName());
        assertEquals(new BigDecimal("3.25"), result.getHoldings().get(1).getWeight());

        assertEquals(2, result.getCountries().size());
        assertEquals("Atlantis", result.getCountries().get(0).getName());
        assertEquals(new BigDecimal("70.00"), result.getCountries().get(0).getWeight());
        assertEquals(1, result.getSectors().size());
        assertEquals("Technology", result.getSectors().get(0).getName());
        assertEquals(new BigDecimal("55.50"), result.getSectors().get(0).getWeight());
    }

    @Test
    void getProfile_handlesMissingSections() {
        String page = """
                <html><body>
                <span data-testid="etf-profile-header_isin-value">XX0000000025</span>
                <div data-testid="etf-profile-header_ter-value">-</div>
                <table data-testid="etf-holdings_top-holdings_table">
                <tr data-testid="etf-holdings_top-holdings_row">
                  <td><a data-testid="tl_etf-holdings_top-holdings_link_name" href="">Gamma Ltd</a></td>
                  <td><span data-testid="tl_etf-holdings_top-holdings_value_percentage">n/a</span></td>
                </tr>
                </table>
                </body></html>
                """;
        EtfProfileAPIService service = this.serviceWithResponse(200, page, new AtomicReference<>());

        EtfProfileDTO result = service.getProfile("XX0000000025").orElseThrow();

        assertEquals("XX0000000025", result.getIsin());
        assertNull(result.getTer());
        assertNull(result.getProvider());
        assertNull(result.getTotalHoldings());
        assertEquals(1, result.getHoldings().size());
        assertEquals("Gamma Ltd", result.getHoldings().get(0).getName());
        assertNull(result.getHoldings().get(0).getIsin());
        assertNull(result.getHoldings().get(0).getWeight());
        assertTrue(result.getCountries().isEmpty());
        assertTrue(result.getSectors().isEmpty());
    }

    @Test
    void getProfile_returnsEmptyWhenPageHasNoIsin() {
        EtfProfileAPIService service = this.serviceWithResponse(200, "<html><body>Search results</body></html>",
                new AtomicReference<>());

        assertTrue(service.getProfile("XX0000000033").isEmpty());
    }

    @Test
    void getProfile_throwsOnApiError() {
        EtfProfileAPIService service = this.serviceWithResponse(500, "error", new AtomicReference<>());

        assertThrows(APIException.class, () -> service.getProfile("XX0000000041"));
    }
}
