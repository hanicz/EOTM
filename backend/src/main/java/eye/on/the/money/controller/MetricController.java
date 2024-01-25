package eye.on.the.money.controller;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.stock.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("metric")
@Slf4j
public class MetricController {

    private final MetricService metricService;

    @Autowired
    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping("/profile/{symbol}")
    public ResponseEntity<Profile> getProfileBySymbol(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getProfileBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/metric/{symbol}")
    public ResponseEntity<Metric> getMetricBySymbol(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getMetricBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/recommendation/{symbol}")
    public ResponseEntity<List<Recommendation>> getRecommendations(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.metricService.getRecommendations(symbol), HttpStatus.OK);
    }
}
