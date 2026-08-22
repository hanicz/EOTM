package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MNBAPIService extends APIService {

    private static final String API = "mnb";

    private static final String SOAP_ACTION = "http://www.mnb.hu/webservices/MNBArfolyamServiceSoap/GetExchangeRates";

    private static final String REQUEST_TEMPLATE = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" \
            xmlns:web="http://www.mnb.hu/webservices/">
              <soap:Body>
                <web:GetExchangeRates>
                  <web:startDate>{0}</web:startDate>
                  <web:endDate>{1}</web:endDate>
                  <web:currencyNames>{2}</web:currencyNames>
                </web:GetExchangeRates>
              </soap:Body>
            </soap:Envelope>""";

    private static final String RESULT_ELEMENT = "GetExchangeRatesResult";

    public static final String HUF = "HUF";

    @Autowired
    public MNBAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                         WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Map<String, NavigableMap<LocalDate, BigDecimal>> getExchangeRates(Collection<String> currencies,
                                                                            LocalDate startDate, LocalDate endDate) {
        log.trace("Enter");
        String currencyNames = currencies.stream()
                .filter(currency -> !HUF.equalsIgnoreCase(currency))
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.joining(","));

        if (currencyNames.isEmpty()) return Map.of();

        String request = MessageFormat.format(REQUEST_TEMPLATE, startDate, endDate, currencyNames);
        ResponseEntity<?> response = this.callPostAPI(this.getApiUrl(MNBAPIService.API), String.class,
                headers -> {
                    headers.setContentType(MediaType.TEXT_XML);
                    headers.add("SOAPAction", MNBAPIService.SOAP_ACTION);
                }, request);

        return this.parseRates(this.unwrapSoapResult((String) response.getBody()));
    }

    private String unwrapSoapResult(String soapResponse) {
        Document envelope = this.parseXml(soapResponse);
        NodeList results = envelope.getElementsByTagNameNS("*", MNBAPIService.RESULT_ELEMENT);
        if (results.getLength() == 0) {
            log.error("No {} element in MNB response", MNBAPIService.RESULT_ELEMENT);
            throw new APIException("Unexpected response from MNB exchange rate service");
        }
        return results.item(0).getTextContent();
    }

    private Map<String, NavigableMap<LocalDate, BigDecimal>> parseRates(String rateDocument) {
        Map<String, NavigableMap<LocalDate, BigDecimal>> ratesByCurrency = new HashMap<>();
        if (rateDocument == null || rateDocument.isBlank()) return ratesByCurrency;

        NodeList days = this.parseXml(rateDocument).getElementsByTagName("Day");
        for (int dayIndex = 0; dayIndex < days.getLength(); dayIndex++) {
            Element day = (Element) days.item(dayIndex);
            LocalDate date = LocalDate.parse(day.getAttribute("date"));

            NodeList rates = day.getElementsByTagName("Rate");
            for (int rateIndex = 0; rateIndex < rates.getLength(); rateIndex++) {
                Element rate = (Element) rates.item(rateIndex);
                String currency = rate.getAttribute("curr");
                ratesByCurrency.computeIfAbsent(currency, key -> new TreeMap<>())
                        .put(date, this.toRatePerUnit(rate));
            }
        }
        return ratesByCurrency;
    }

    private BigDecimal toRatePerUnit(Element rate) {
        String value = rate.getTextContent().trim().replace(',', '.');
        String unit = rate.getAttribute("unit");
        BigDecimal quoted = new BigDecimal(value);
        return (unit == null || unit.isBlank()) ? quoted
                : quoted.divide(new BigDecimal(unit), MathContext.DECIMAL64);
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Unable to parse MNB response: {}", e.getMessage());
            throw new APIException("Unable to parse response from MNB exchange rate service");
        }
    }
}
