package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("metric")
@Slf4j
public class MetricController {

    @Autowired
    private MetricService metricService;

    @GetMapping("/profile/{symbol}")
    public ResponseEntity<Profile> getProfileBySymbol(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        log.trace("Enter getProfileBySymbol");
        return new ResponseEntity<Profile>(this.metricService.getProfileBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/metric/{symbol}")
    public ResponseEntity<Metric> getMetricBySymbol(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        log.trace("Enter getMetricBySymbol");
        return new ResponseEntity<Metric>(this.metricService.getMetricBySymbol(symbol), HttpStatus.OK);
    }

    @GetMapping("/recommendation/{symbol}")
    public ResponseEntity<List<Recommendation>> getRecommendations(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        log.trace("Enter getRecommendations");
        return new ResponseEntity<List<Recommendation>>(this.metricService.getRecommendations(symbol), HttpStatus.OK);
    }
}
