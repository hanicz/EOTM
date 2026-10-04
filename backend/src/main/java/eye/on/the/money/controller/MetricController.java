package eye.on.the.money.controller;

import eye.on.the.money.dto.out.EtfProfileDTO;
import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.service.stock.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("api/v1/metric")
@RequiredArgsConstructor
public class MetricController {

    private static final Pattern ISIN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");

    private final MetricService metricService;


    @GetMapping("/profile/{symbol}")
    public ResponseEntity<ProfileDTO> getProfileBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(this.metricService.getProfileBySymbol(symbol));
    }

    @GetMapping("/metric/{symbol}")
    public ResponseEntity<MetricDTO> getMetricBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(this.metricService.getMetricBySymbol(symbol));
    }

    @GetMapping("/etf/{isin}")
    public ResponseEntity<EtfProfileDTO> getEtfProfile(@PathVariable String isin) {
        if (!ISIN.matcher(isin).matches()) {
            throw new ValidationException("Invalid ISIN");
        }
        return ResponseEntity.ok(this.metricService.getEtfProfile(isin));
    }

    @GetMapping("/recommendation/{symbol}")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(@PathVariable String symbol) {
        return ResponseEntity.ok(this.metricService.getRecommendations(symbol));
    }
}
