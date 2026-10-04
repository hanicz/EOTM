package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.EtfProfileDTO;
import eye.on.the.money.dto.out.EtfWeightDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EtfProfileAPIService extends APIService {

    private final static String API = "etf";

    private final static String PROFILE_PATH = "/etf-profile.html?isin={0}";
    private final static String USER_AGENT = "Mozilla/5.0";

    @Autowired
    public EtfProfileAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                                WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Optional<EtfProfileDTO> getProfile(String isin) {
        String url = this.expandTemplate(this.getApiUrl(API) + PROFILE_PATH, new Object[]{this.encodeQuery(isin)});
        ResponseEntity<String> response = this.callGetAPI(url, String.class,
                headers -> headers.set(HttpHeaders.USER_AGENT, USER_AGENT));
        return this.parse(response.getBody());
    }

    Optional<EtfProfileDTO> parse(String html) {
        Document document = Jsoup.parse(html);
        String isin = this.text(document, "etf-profile-header_isin-value");
        if (isin == null) {
            return Optional.empty();
        }
        return Optional.of(EtfProfileDTO.builder()
                .isin(isin)
                .provider(this.text(document, "tl_etf-basics_value_fund-provider"))
                .indexName(this.text(document, "tl_etf-basics_value_index-name"))
                .investmentFocus(this.text(document, "tl_etf-basics_value_investment-focus"))
                .fundSize(this.text(document, "etf-profile-header_fund-size-value-wrapper"))
                .ter(this.text(document, "etf-profile-header_ter-value"))
                .replication(this.text(document, "etf-profile-header_replication-value"))
                .distributionPolicy(this.text(document, "etf-profile-header_distribution-policy-value"))
                .fundCurrency(this.text(document, "tl_etf-basics_value_fund-currency"))
                .currencyHedge(this.text(document, "tl_etf-basics_value_currency-hedge"))
                .inceptionDate(this.text(document, "etf-profile-header_inception-date-value"))
                .volatility(this.text(document, "tl_etf-basics_value_volatility"))
                .totalHoldings(this.parseCount(this.text(document, "etf-profile-header_holdings-value")))
                .holdings(this.parseHoldings(document))
                .countries(this.parseBreakdown(document, "countries"))
                .sectors(this.parseBreakdown(document, "sectors"))
                .build());
    }

    private List<EtfWeightDTO> parseHoldings(Document document) {
        List<EtfWeightDTO> holdings = new ArrayList<>();
        for (Element row : document.select(this.byTestId("etf-holdings_top-holdings_row"))) {
            Element link = row.selectFirst(this.byTestId("tl_etf-holdings_top-holdings_link_name"));
            Element percentage = row.selectFirst(this.byTestId("tl_etf-holdings_top-holdings_value_percentage"));
            if (link == null) {
                continue;
            }
            String name = link.hasAttr("title") ? link.attr("title").trim() : link.text().trim();
            String href = link.attr("href");
            String isin = href.contains("/") ? href.substring(href.lastIndexOf('/') + 1) : null;
            holdings.add(EtfWeightDTO.builder()
                    .name(name)
                    .isin(isin == null || isin.isBlank() ? null : isin)
                    .weight(percentage == null ? null : this.parsePercent(percentage.text()))
                    .build());
        }
        return holdings;
    }

    private List<EtfWeightDTO> parseBreakdown(Document document, String section) {
        List<EtfWeightDTO> breakdown = new ArrayList<>();
        Element table = document.selectFirst(this.byTestId("etf-holdings_" + section + "_table"));
        if (table == null) {
            return breakdown;
        }
        Elements names = table.select(this.byTestId("tl_etf-holdings_" + section + "_value_name"));
        Elements percentages = table.select(this.byTestId("tl_etf-holdings_" + section + "_value_percentage"));
        for (int index = 0; index < Math.min(names.size(), percentages.size()); index++) {
            breakdown.add(EtfWeightDTO.builder()
                    .name(names.get(index).text().trim())
                    .weight(this.parsePercent(percentages.get(index).text()))
                    .build());
        }
        return breakdown;
    }

    private String text(Document document, String testId) {
        Element element = document.selectFirst(this.byTestId(testId));
        if (element == null) {
            return null;
        }
        String value = element.text().trim();
        return value.isEmpty() || "-".equals(value) ? null : value;
    }

    private String byTestId(String testId) {
        return "[data-testid=" + testId + "]";
    }

    private BigDecimal parsePercent(String value) {
        String cleaned = value.replace("%", "").replace(",", "").trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseCount(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : Integer.valueOf(digits);
    }
}
