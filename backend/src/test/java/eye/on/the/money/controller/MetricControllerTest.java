package eye.on.the.money.controller;

import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.service.stock.MetricService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MetricControllerTest {

    @Mock
    private MetricService metricService;

    @InjectMocks
    private MetricController metricController;

    @Test
    public void getProfileBySymbol() {
        ProfileDTO profileDTO = ProfileDTO.builder()
                .ticker("t")
                .shareOutstanding(500L)
                .name("n")
                .weburl("w")
                .ipo(LocalDate.now())
                .logo("l")
                .marketCapitalization(0.6)
                .exchange("e")
                .build();
        when(this.metricService.getProfileBySymbol(anyString())).thenReturn(profileDTO);

        ResponseEntity<ProfileDTO> result = this.metricController.getProfileBySymbol("symbol");

        Assertions.assertEquals(profileDTO, result.getBody());
    }

    @Test
    public void getMetricBySymbol() {
        MetricDTO metricDTO = MetricDTO.builder()
                .yearLow(0.1)
                .yearLowDate(LocalDate.now())
                .threeMonthAverageTradingVolume(55.0)
                .yearHigh(77.8)
                .yearHighDate(LocalDate.now())
                .tenDayAverageTradingVolume(78888.0)
                .peInclExtraTTM(3.5)
                .build();
        when(this.metricService.getMetricBySymbol(anyString())).thenReturn(metricDTO);

        ResponseEntity<MetricDTO> result = this.metricController.getMetricBySymbol("symbol");

        Assertions.assertEquals(metricDTO, result.getBody());
    }

    @Test
    public void getRecommendations() {
        List<RecommendationDTO> recs = new ArrayList<>();
        recs.add(RecommendationDTO.builder().symbol("s").sell(5).period(LocalDate.now()).hold(7).strongSell(11).strongBuy(22).build());
        recs.add(RecommendationDTO.builder().symbol("s").sell(10).period(LocalDate.now()).hold(22).strongSell(2).strongBuy(1).build());
        recs.add(RecommendationDTO.builder().symbol("s").sell(12).period(LocalDate.now()).hold(44).strongSell(78).strongBuy(0).build());


        when(this.metricService.getRecommendations(anyString())).thenReturn(recs);

        ResponseEntity<List<RecommendationDTO>> result = this.metricController.getRecommendations("symbol");

        Assertions.assertIterableEquals(recs, result.getBody());
    }
}