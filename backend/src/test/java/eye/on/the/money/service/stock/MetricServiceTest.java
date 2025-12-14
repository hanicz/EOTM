package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.service.api.StockMetricAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock
    private StockMetricAPIService stockMetricAPIService;

    @InjectMocks
    private MetricService metricService;

    @Test
    public void getProfileBySymbol() {
        ProfileDTO profileDTO = ProfileDTO.builder().ipo(LocalDate.now()).country("country").logo("logo")
                .name("name").currency("currency").exchange("exchange").finnhubIndustry("industry")
                .marketCapitalization(1.0).ticker("ticket").weburl("webUrl").shareOutstanding(1L).build();
        String[] peers = new String[]{"peer1", "peer2"};
        when(this.stockMetricAPIService.getProfile("symbol")).thenReturn(profileDTO);
        when(this.stockMetricAPIService.getPeers("symbol")).thenReturn(peers);

        ProfileDTO result = this.metricService.getProfileBySymbol("symbol");
        Assertions.assertEquals(profileDTO, result);
    }

    @Test
    public void getMetricBySymbol() {
        MetricDTO metricDTO = MetricDTO.builder().peInclExtraTTM(1.0).tenDayAverageTradingVolume(2.0)
                .yearHighDate(LocalDate.now()).yearHigh(3.0).yearLowDate(LocalDate.now()).yearLow(4.0)
                .threeMonthAverageTradingVolume(5.0).build();
        when(this.stockMetricAPIService.getMetric("symbol")).thenReturn(metricDTO);

        MetricDTO result = this.metricService.getMetricBySymbol("symbol");
        Assertions.assertEquals(metricDTO, result);
    }

    @Test
    public void getRecommendations() {
        List<RecommendationDTO> recommendationDTOS = new ArrayList<>();
        recommendationDTOS.add(RecommendationDTO.builder().buy(1).hold(1).strongBuy(4)
                .strongSell(3).sell(10).strongSell(156).period(LocalDate.now()).symbol("symbol").build());
        recommendationDTOS.add(RecommendationDTO.builder().buy(14).hold(411).strongBuy(120)
                .strongSell(2).sell(10).strongSell(156).period(LocalDate.now()).symbol("symbol").build());
        recommendationDTOS.add(RecommendationDTO.builder().buy(12).hold(321).strongBuy(99)
                .strongSell(77).sell(66).strongSell(123).period(LocalDate.now()).symbol("symbol").build());
        when(this.stockMetricAPIService.getRecommendations("symbol")).thenReturn(recommendationDTOS);

        List<RecommendationDTO> result = this.metricService.getRecommendations("symbol");
        Assertions.assertEquals(recommendationDTOS, result);
    }
}