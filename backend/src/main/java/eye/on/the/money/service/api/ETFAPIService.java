package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.ETFInvestmentDTO;

import java.util.List;

public interface ETFAPIService {
    public void getLiveValue(List<ETFInvestmentDTO> investmentDTOList);
}
