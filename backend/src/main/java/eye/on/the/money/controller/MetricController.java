package eye.on.the.money.controller;

import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.service.stock.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/metric")
@Slf4j
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;


    @GetMapping("/profile/{symbol}")
    public ResponseEntity<ProfileDTO> getProfileBySymbol(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getProfileBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/metric/{symbol}")
    public ResponseEntity<MetricDTO> getMetricBySymbol(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getMetricBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/recommendation/{symbol}")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getRecommendations(symbol), HttpStatus.OK);
    }
}
