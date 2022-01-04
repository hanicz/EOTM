package eye.on.the.money.service.impl;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.repository.DividendRepository;
import eye.on.the.money.service.DividendService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
class DividendServiceImplTest {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private DividendService dividendService;

    @Test
    public void getDividends() {
        List<DividendDTO> dividends = this.dividendService.getDividends(1L);
        List<Dividend> dividendsActual = this.dividendRepository.findByUser_IdOrderByDividendDate(1L);
        assertEquals(dividendsActual.size(), dividends.size());
    }
}