package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.service.api.StockMetricAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricService {

    private final StockMetricAPIService stockMetricAPIService;

    public ProfileDTO getProfileBySymbol(String symbol) {
        log.trace("Enter");
        ProfileDTO profileDTO = this.stockMetricAPIService.getProfile(symbol);
        profileDTO.setPeers(Arrays.asList(this.stockMetricAPIService.getPeers(symbol)));
        return profileDTO;
    }

    public MetricDTO getMetricBySymbol(String symbol) {
        log.trace("Enter");
        return this.stockMetricAPIService.getMetric(symbol);
    }

    public List<RecommendationDTO> getRecommendations(String symbol) {
        log.trace("Enter");
        List<RecommendationDTO> recommendationDTOS = this.stockMetricAPIService.getRecommendations(symbol);
        recommendationDTOS.sort(Comparator.comparing(RecommendationDTO::getPeriod));
        return recommendationDTOS;
    }
}
