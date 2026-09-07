package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.service.api.StockMetricAPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final StockMetricAPIService stockMetricAPIService;

    public ProfileDTO getProfileBySymbol(String symbol) {
        ProfileDTO profileDTO = this.stockMetricAPIService.getProfile(symbol);
        profileDTO.setPeers(Arrays.asList(this.stockMetricAPIService.getPeers(symbol)));
        return profileDTO;
    }

    public MetricDTO getMetricBySymbol(String symbol) {
        return this.stockMetricAPIService.getMetric(symbol);
    }

    public List<RecommendationDTO> getRecommendations(String symbol) {
        List<RecommendationDTO> recommendationDTOS = this.stockMetricAPIService.getRecommendations(symbol);
        recommendationDTOS.sort(Comparator.comparing(RecommendationDTO::getPeriod));
        return recommendationDTOS;
    }
}
