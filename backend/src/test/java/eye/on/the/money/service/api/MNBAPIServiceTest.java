package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MNBAPIServiceTest {

    private static final String API = "mnb";

    /**
     * Verbatim response from the live service for 2024-01-02..2024-01-05, EUR and USD. Note the payload
     * arrives as an escaped string inside GetExchangeRatesResult and the days are newest first.
     */
    private static final String RATES_RESPONSE = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"><s:Body>\
            <GetExchangeRatesResponse xmlns="http://www.mnb.hu/webservices/">\
            <GetExchangeRatesResult>&lt;MNBExchangeRates&gt;\
            &lt;Day date="2024-01-05"&gt;&lt;Rate unit="1" curr="EUR"&gt;378,12&lt;/Rate&gt;\
            &lt;Rate unit="1" curr="USD"&gt;346,49&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;Day date="2024-01-04"&gt;&lt;Rate unit="1" curr="EUR"&gt;379,38&lt;/Rate&gt;\
            &lt;Rate unit="1" curr="USD"&gt;345,90&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;Day date="2024-01-03"&gt;&lt;Rate unit="1" curr="EUR"&gt;380,78&lt;/Rate&gt;\
            &lt;Rate unit="1" curr="USD"&gt;348,16&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;Day date="2024-01-02"&gt;&lt;Rate unit="1" curr="EUR"&gt;382,12&lt;/Rate&gt;\
            &lt;Rate unit="1" curr="USD"&gt;346,91&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;/MNBExchangeRates&gt;</GetExchangeRatesResult></GetExchangeRatesResponse></s:Body></s:Envelope>""";

    /**
     * Verbatim response for 2024-03-28..2024-04-02, JPY and EUR. Good Friday, the weekend and Easter
     * Monday are simply missing, and JPY is quoted per 100 units.
     */
    private static final String EASTER_RESPONSE = """
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"><s:Body>\
            <GetExchangeRatesResponse xmlns="http://www.mnb.hu/webservices/">\
            <GetExchangeRatesResult>&lt;MNBExchangeRates&gt;\
            &lt;Day date="2024-04-02"&gt;&lt;Rate unit="1" curr="EUR"&gt;395,26&lt;/Rate&gt;\
            &lt;Rate unit="100" curr="JPY"&gt;242,88&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;Day date="2024-03-28"&gt;&lt;Rate unit="1" curr="EUR"&gt;395,83&lt;/Rate&gt;\
            &lt;Rate unit="100" curr="JPY"&gt;242,53&lt;/Rate&gt;&lt;/Day&gt;\
            &lt;/MNBExchangeRates&gt;</GetExchangeRatesResult></GetExchangeRatesResponse></s:Body></s:Envelope>""";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
    }

    private MNBAPIService serviceWithResponse(String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.TEXT_XML_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new MNBAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    private void stubConfig() {
        when(this.configRepository.findById(API))
                .thenReturn(Optional.of(new Config(API, "http://www.mnb.hu/arfolyamok.asmx")));
    }

    @Test
    void getExchangeRates_unwrapsEscapedPayloadAndParsesCommaDecimals() {
        this.stubConfig();
        MNBAPIService service = this.serviceWithResponse(RATES_RESPONSE);

        Map<String, NavigableMap<LocalDate, BigDecimal>> rates =
                service.getExchangeRates(List.of("EUR", "USD"), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 5));

        assertEquals(2, rates.size());
        assertEquals(0, new BigDecimal("382.12").compareTo(rates.get("EUR").get(LocalDate.of(2024, 1, 2))));
        assertEquals(0, new BigDecimal("346.49").compareTo(rates.get("USD").get(LocalDate.of(2024, 1, 5))));
    }

    @Test
    void getExchangeRates_ordersDaysAscendingDespiteNewestFirstResponse() {
        this.stubConfig();
        MNBAPIService service = this.serviceWithResponse(RATES_RESPONSE);

        NavigableMap<LocalDate, BigDecimal> eur =
                service.getExchangeRates(List.of("EUR"), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 5)).get("EUR");

        assertEquals(LocalDate.of(2024, 1, 2), eur.firstKey());
        assertEquals(LocalDate.of(2024, 1, 5), eur.lastKey());
    }

    @Test
    void getExchangeRates_dividesByQuotedUnit() {
        this.stubConfig();
        MNBAPIService service = this.serviceWithResponse(EASTER_RESPONSE);

        NavigableMap<LocalDate, BigDecimal> jpy =
                service.getExchangeRates(List.of("JPY"), LocalDate.of(2024, 3, 28), LocalDate.of(2024, 4, 2)).get("JPY");

        // 242,88 HUF per 100 JPY is 2.4288 HUF per JPY.
        assertEquals(0, new BigDecimal("2.4288").compareTo(jpy.get(LocalDate.of(2024, 4, 2))));
    }

    @Test
    void getExchangeRates_omitsNonBankingDaysSoLookupCanFallBack() {
        this.stubConfig();
        MNBAPIService service = this.serviceWithResponse(EASTER_RESPONSE);

        NavigableMap<LocalDate, BigDecimal> eur =
                service.getExchangeRates(List.of("EUR"), LocalDate.of(2024, 3, 28), LocalDate.of(2024, 4, 2)).get("EUR");

        // Easter Monday is not published at all.
        assertFalse(eur.containsKey(LocalDate.of(2024, 4, 1)));
        // The last rate published before it is Maundy Thursday's.
        assertEquals(LocalDate.of(2024, 3, 28), eur.floorKey(LocalDate.of(2024, 4, 1)));
    }

    @Test
    void getExchangeRates_skipsCallWhenOnlyHufRequested() {
        MNBAPIService service = this.serviceWithResponse(RATES_RESPONSE);

        // No config stub: reaching the network at all would fail this test.
        assertTrue(service.getExchangeRates(List.of("HUF"), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 5)).isEmpty());
    }

    @Test
    void getExchangeRates_throwsWhenResultElementMissing() {
        this.stubConfig();
        MNBAPIService service = this.serviceWithResponse("""
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"><s:Body>\
                <s:Fault><faultstring>Invalid date interval</faultstring></s:Fault></s:Body></s:Envelope>""");

        assertThrows(APIException.class, () -> service.getExchangeRates(List.of("EUR"),
                LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 5)));
    }
}
