package eye.on.the.money.service.currency;

import eye.on.the.money.dto.out.InvestmentDTO;

import java.util.List;

public interface CurrencyConverter {
    public void changeInvestmentsCurrency(List<InvestmentDTO> investments, String toCurrency);
}
