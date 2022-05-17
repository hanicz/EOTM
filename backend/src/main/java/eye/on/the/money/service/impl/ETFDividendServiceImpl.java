package eye.on.the.money.service.impl;

import eye.on.the.money.repository.etf.ETFDividendRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.ETFDividendService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ETFDividendServiceImpl implements ETFDividendService {

    @Autowired
    private ETFDividendRepository etfDividendRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private ModelMapper modelMapper;
}
