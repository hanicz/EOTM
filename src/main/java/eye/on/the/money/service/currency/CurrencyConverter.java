package eye.on.the.money.service.currency;

import eye.on.the.money.dto.InvestmentDTO;

import java.util.List;

public interface CurrencyConverter {
    public void changeInvestmentsToCurrency(List<InvestmentDTO> investments, String toCurrency);
}
