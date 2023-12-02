package eye.on.the.money.controller;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.stock.MetricService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class MetricControllerTest {

    @Mock
    private MetricService metricService;

    @InjectMocks
    private MetricController metricController;

    @Test
    public void getProfileBySymbol() {
        Profile profile = Profile.builder()
                .ticker("t")
                .shareOutstanding(500L)
                .name("n")
                .weburl("w")
                .ipo(new Date())
                .logo("l")
                .marketCapitalization(0.6)
                .exchange("e")
                .build();
        when(this.metricService.getProfileBySymbol(anyString())).thenReturn(profile);

        ResponseEntity<Profile> result = this.metricController.getProfileBySymbol("symbol");

        Assertions.assertEquals(profile, result.getBody());
    }

    @Test
    public void getMetricBySymbol() {
        Metric metric = Metric.builder()
                .yearLow(0.1)
                .yearLowDate(new Date())
                .threeMonthAverageTradingVolume(55.0)
                .yearHigh(77.8)
                .yearHighDate(new Date())
                .tenDayAverageTradingVolume(78888.0)
                .peInclExtraTTM(3.5)
                .build();
        when(this.metricService.getMetricBySymbol(anyString())).thenReturn(metric);

        ResponseEntity<Metric> result = this.metricController.getMetricBySymbol("symbol");

        Assertions.assertEquals(metric, result.getBody());
    }

    @Test
    public void getRecommendations() {
        List<Recommendation> recs = new ArrayList<>();
        recs.add(Recommendation.builder().symbol("s").sell(5).period(new Date()).hold(7).strongSell(11).strongBuy(22).build());
        recs.add(Recommendation.builder().symbol("s").sell(10).period(new Date()).hold(22).strongSell(2).strongBuy(1).build());
        recs.add(Recommendation.builder().symbol("s").sell(12).period(new Date()).hold(44).strongSell(78).strongBuy(0).build());


        when(this.metricService.getRecommendations(anyString())).thenReturn(recs);

        ResponseEntity<List<Recommendation>> result = this.metricController.getRecommendations("symbol");

        Assertions.assertIterableEquals(recs, result.getBody());
    }
}