package eye.on.the.money.service.stock.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.api.StockMetricAPIService;
import eye.on.the.money.service.stock.MetricService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class MetricServiceTest {


    @Mock
    private StockMetricAPIService stockMetricAPIService;

    @InjectMocks
    private MetricService metricService;

    @Test
    public void getProfileBySymbol() {
        Profile profile = Profile.builder().ipo(new Date()).country("country").logo("logo")
                .name("name").currency("currency").exchange("exchange").finnhubIndustry("industry")
                .marketCapitalization(1.0).ticker("ticket").weburl("webUrl").shareOutstanding(1L).build();
        String[] peers = new String[]{"peer1", "peer2"};
        when(this.stockMetricAPIService.getProfile("symbol")).thenReturn(profile);
        when(this.stockMetricAPIService.getPeers("symbol")).thenReturn(peers);

        Profile result = this.metricService.getProfileBySymbol("symbol");
        Assertions.assertEquals(profile, result);
    }

    @Test
    public void getMetricBySymbol() {
        Metric metric = Metric.builder().peInclExtraTTM(1.0).tenDayAverageTradingVolume(2.0)
                .yearHighDate(new Date()).yearHigh(3.0).yearLowDate(new Date()).yearLow(4.0)
                .threeMonthAverageTradingVolume(5.0).build();
        when(this.stockMetricAPIService.getMetric("symbol")).thenReturn(metric);

        Metric result = this.metricService.getMetricBySymbol("symbol");
        Assertions.assertEquals(metric, result);
    }

    @Test
    public void getRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        recommendations.add(Recommendation.builder().buy(1).hold(1).strongBuy(4)
                .strongSell(3).sell(10).strongSell(156).period(new Date()).symbol("symbol").build());
        recommendations.add(Recommendation.builder().buy(14).hold(411).strongBuy(120)
                .strongSell(2).sell(10).strongSell(156).period(new Date()).symbol("symbol").build());
        recommendations.add(Recommendation.builder().buy(12).hold(321).strongBuy(99)
                .strongSell(77).sell(66).strongSell(123).period(new Date()).symbol("symbol").build());
        when(this.stockMetricAPIService.getRecommendations("symbol")).thenReturn(recommendations);

        List<Recommendation> result = this.metricService.getRecommendations("symbol");
        Assertions.assertEquals(recommendations, result);
    }
}